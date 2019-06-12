package com.eroelf.tfserver.model;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.eroelf.javaxsx.util.Strings;
import com.eroelf.tfserver.data.ArrayWrapper;
import com.eroelf.tfserver.data.Sample;
import com.eroelf.tfserver.exception.UnregisteredVersionException;
import com.eroelf.tfserver.model.handler.ModelHandler;

/**
 * This class maintains a groups of {@link ModelHandler}s under the same model name but in different versions.
 * 
 * @author weikun.zhong
 */
public final class ModelGroup implements Closeable
{
	private String dirBase;
	private final ExecutorService es;
	private String modelName;
	private Map<String, ModelHandler> versionMap=new HashMap<>();
	private ModelHandler defaultHandler;

	private final Object lock=new Object();

	public ModelGroup(String dirBase, ExecutorService es)
	{
		this.dirBase=dirBase;
		this.es=es;
		Path path=Paths.get(dirBase);
		this.modelName=path.getName(path.getNameCount()-1).toString();
	}

	public Set<String> getVersions()
	{
		Set<String> set=null;
		synchronized(lock)
		{
			set=versionMap.keySet();
		}
		return Collections.unmodifiableSet(set);
	}

	public boolean containsVersion(String version)
	{
		return versionMap.containsKey(version);
	}

	public void put(String version, Properties defaultProperties) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException
	{
		checkStatus(null);
		String versionDir=Paths.get(dirBase, version).toString();
		Properties properties=ModelHelper.loadConfigQuiet(versionDir, "conf");
		properties=ModelHelper.complementFromDefault(properties, defaultProperties);
		ModelHandler newHandler=(ModelHandler)Class.forName(properties.getProperty("handler_class")).newInstance();
		newHandler.load(es, versionDir, properties);
		synchronized(lock)
		{
			checkStatus(newHandler);
			ModelHandler oldHandler=versionMap.put(version, newHandler);
			if(oldHandler!=null && defaultHandler!=oldHandler)
				es.execute(() -> {
					try
					{
						oldHandler.close();
					}
					catch(IOException e)
					{
						throw new UncheckedIOException(e);
					}
				});
		}
	}

	public void remove(String version) throws IOException
	{
		synchronized(lock)
		{
			checkStatus(null);
			ModelHandler oldHandler=versionMap.remove(version);
			if(oldHandler!=null && defaultHandler!=oldHandler)
				es.execute(() -> {
					try
					{
						oldHandler.close();
					}
					catch(IOException e)
					{
						throw new UncheckedIOException(e);
					}
				});
		}
	}

	public boolean setDefault(String defaultVersion) throws IOException
	{
		ModelHandler handler=versionMap.get(defaultVersion);
		ModelHandler oldHandler=defaultHandler;
		if(handler==oldHandler)
			return true;
		if(handler!=null)
		{
			synchronized(lock)
			{
				checkStatus(null);
				defaultHandler=handler;
				if(oldHandler!=null && !versionMap.values().contains(oldHandler))
					es.execute(() -> {
						try
						{
							oldHandler.close();
						}
						catch(IOException e)
						{
							throw new UncheckedIOException(e);
						}
					});
			}
			return true;
		}
		else
		{
			synchronized(lock)
			{
				checkStatus(null);
				Collection<ModelHandler> collection=versionMap.values();
				handler=collection.stream().findAny().orElse(null);
				if(handler==oldHandler)
					return false;
				defaultHandler=handler;
				if(oldHandler!=null && !collection.contains(oldHandler))
					es.execute(() -> {
						try
						{
							oldHandler.close();
						}
						catch(IOException e)
						{
							throw new UncheckedIOException(e);
						}
					});
			}
			return false;
		}
	}

	public Future<?> update(Set<String> updateVersions, Set<String> removeVersions, Properties defaultProperties, String defaultVersion)
	{
		CountDownLatch latch=new CountDownLatch(updateVersions.size()+removeVersions.size());
		for(String version : updateVersions)
		{
			es.execute(() -> {
				try
				{
					put(version, defaultProperties);
				}
				catch(ClassNotFoundException|InstantiationException|IllegalAccessException|IOException e)
				{
					throw new RuntimeException("Exception throwed during model updating with name:version="+modelName+":"+version, e);
				}
				finally
				{
					latch.countDown();
				}
			});
		}
		for(String version : removeVersions)
		{
			es.execute(() -> {
				try
				{
					remove(version);
				}
				catch(IOException e)
				{
					throw new RuntimeException("Exception throwed during model removing with name:version="+modelName+":"+version, e);
				}
				finally
				{
					latch.countDown();
				}
			});
		}
		return es.submit(() -> {
			try
			{
				latch.await();
				setDefault(defaultVersion);
			}
			catch(IOException e)
			{
				throw new UncheckedIOException(e);
			}
			catch(InterruptedException e)
			{
				throw new RuntimeException("Exception throwed during set default version for model: "+modelName, e);
			}
		});
	}

	public Map<String, ArrayWrapper> run(String modelVersion, Sample sample, Class<? extends ArrayWrapper> outputClass) throws InterruptedException
	{
		ModelHandler handler;
		if(Strings.isValid(modelVersion))
			handler=versionMap.get(modelVersion);
		else
			handler=defaultHandler;
		if(handler!=null)
			return handler.run(sample, outputClass);
		else
			throw new UnregisteredVersionException(String.format("Unregistered version: %s. Must in %s, or keep empty for default.", modelVersion, versionMap.keySet().toString()));
	}

	@Override
	public void close() throws IOException
	{
		CountDownLatch latch;
		Iterator<Entry<String, ModelHandler>> iter;
		ModelHandler handler;
		synchronized(lock)
		{
			if(versionMap!=null)
			{
				latch=new CountDownLatch(versionMap.size()+1);
				iter=versionMap.entrySet().iterator();
				handler=defaultHandler;
				versionMap=null;
				defaultHandler=null;
			}
			else
				return;
		}
		while(iter.hasNext())
		{
			Entry<String, ModelHandler> entry=iter.next();
			iter.remove();
			es.execute(() -> {
				try
				{
					entry.getValue().close();
				}
				catch(IOException e)
				{
					throw new UncheckedIOException(e);
				}
				finally
				{
					latch.countDown();
				}
			});
		}
		es.execute(() -> {
			try
			{
				if(handler!=null)
					handler.close();
			}
			catch(IOException e)
			{
				throw new UncheckedIOException(e);
			}
			finally
			{
				latch.countDown();
			}
		});
		try
		{
			latch.await();
		}
		catch(InterruptedException e)
		{
			throw new RuntimeException("Close has been interrupted!", e);
		}
	}

	private void checkStatus(Closeable resource) throws IOException
	{
		if(versionMap==null)
		{
			if(resource!=null)
				resource.close();
			throw new IllegalAccessError("This model group has been closed!");
		}
	}
}

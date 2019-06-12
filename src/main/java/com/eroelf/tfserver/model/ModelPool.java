package com.eroelf.tfserver.model;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eroelf.javaxsx.util.FileSysUtil;
import com.eroelf.tfserver.data.ArrayWrapper;
import com.eroelf.tfserver.data.Sample;
import com.eroelf.tfserver.exception.UnregisteredModelException;

/**
 * This class maintains a pool of different {@link ModelGroup}s.
 * 
 * @author weikun.zhong
 */
public final class ModelPool implements Closeable
{
	private static final Logger LOGGER=LoggerFactory.getLogger(ModelPool.class);

	private String baseDir;
	private Properties defaultProperties;
	private final ExecutorService es;
	private Map<String, ModelGroup> modelGroupMap=new HashMap<>();
	private long lastCheckedTime=0;

	private final Object lock=new Object();

	public ModelPool(String baseDir, Properties defaultModelHandlerProperties)
	{
		this.baseDir=baseDir;
		this.defaultProperties=defaultModelHandlerProperties;
		es=Executors.newCachedThreadPool();// Do NOT use fixed size thread pool!
	}

	public void update()
	{
		synchronized(lock)
		{
			if(modelGroupMap!=null)
			{
				List<Future<?>> futures=new ArrayList<>();
				long theLastCheckedTime=lastCheckedTime;
				lastCheckedTime=new Date().getTime();
				List<File> modelGroupDirs=FileSysUtil.getFiles(baseDir, false, true);
				Set<String> currModelNames=new HashSet<>(modelGroupMap.keySet());
				for(File group : modelGroupDirs)
				{
					String groupName=null;
					String groupPath=null;
					try
					{
						groupName=group.getName();
						groupPath=group.getAbsolutePath();
						Properties properties=ModelHelper.loadConfigQuiet(groupPath, "conf");
						properties=ModelHelper.complementFromDefault(properties, defaultProperties);
						List<File> modelVersionDirs=FileSysUtil.getFiles(groupPath, false, true);
						String defaultVersion=modelVersionDirs.stream().reduce((a, b) -> {return a.lastModified()<b.lastModified() ? b : a;}).map(file -> file.getName()).orElse(null);
						Set<String> updateSet=modelVersionDirs.stream().map(file -> file.getName()).collect(Collectors.toSet());
						Set<String> removeSet;
						if(modelGroupMap.containsKey(groupName))
						{
							ModelGroup modelGroup=modelGroupMap.get(groupName);
							removeSet=new HashSet<>(modelGroup.getVersions());
							removeSet.removeAll(updateSet);
							for(File version : modelVersionDirs)
							{
								if(modelGroup.containsVersion(version.getName()) && version.lastModified()<=theLastCheckedTime)
									updateSet.remove(version.getName());
							}
							futures.add(modelGroup.update(updateSet, removeSet, properties, defaultVersion));
						}
						else
						{
							ModelGroup modelGroup=new ModelGroup(groupPath, es);
							removeSet=new HashSet<>();
							futures.add(modelGroup.update(updateSet, removeSet, properties, defaultVersion));
							modelGroupMap.put(groupName, modelGroup);
						}
						currModelNames.remove(groupName);
						LOGGER.info(String.format("ModelGroup \"%s\" has been checked with %s versions submitted for updating and %s versions submitted for removing, the default version will be set to (%s).", groupName, updateSet.toString(), removeSet.toString(), defaultVersion));
					}
					catch(Exception e)
					{
						LOGGER.error(String.format("ModelGroup \"%s\" updating failed!", groupName), e);
					}
				}
				if(!currModelNames.isEmpty())
				{
					for(String dueModel : currModelNames)
					{
						futures.add(es.submit(() -> {
							try
							{
								modelGroupMap.remove(dueModel).close();
							}
							catch(IOException e)
							{
								throw new UncheckedIOException(e);
							}
						}));
					}
				}
				LOGGER.info(String.format("ModelGroup %s has been submitted for removing.", currModelNames.toString()));
				for(Future<?> future : futures)
				{
					try
					{
						future.get();
					}
					catch(InterruptedException|ExecutionException e)
					{
						LOGGER.error("An exception occurred during waiting one model updating", e);
					}
				}
				LOGGER.info("ModelPool updated.");
			}
		}
	}

	public Map<String, ArrayWrapper> run(String modelName, String modelVersion, Sample sample, Class<? extends ArrayWrapper> outputClass) throws InterruptedException
	{
		ModelGroup group=modelGroupMap.get(modelName);
		if(group!=null)
			return group.run(modelVersion, sample, outputClass);
		else
			throw new UnregisteredModelException(String.format("Unregistered model: %s. Must in %s.", modelName, modelGroupMap.keySet().toString()));
	}

	@Override
	public void close() throws IOException
	{
		synchronized(lock)
		{
			if(modelGroupMap==null)
				return;
			CountDownLatch latch=new CountDownLatch(modelGroupMap.size());
			Iterator<Entry<String, ModelGroup>> iter=modelGroupMap.entrySet().iterator();
			modelGroupMap=null;
			while(iter.hasNext())
			{
				Entry<String, ModelGroup> entry=iter.next();
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
			try
			{
				latch.await();
				es.shutdown();
				int failedCount=0;
				while(!es.awaitTermination(1, TimeUnit.MINUTES))
				{
					if(failedCount++>10)
					{
						LOGGER.warn(String.format("Failed to shutdown the es in %d attempts!", failedCount));
						es.shutdownNow();
					}
				}
			}
			catch(InterruptedException e)
			{
				throw new RuntimeException("ModelPool close has been interrupted and not finished!", e);
			}
		}
	}
}

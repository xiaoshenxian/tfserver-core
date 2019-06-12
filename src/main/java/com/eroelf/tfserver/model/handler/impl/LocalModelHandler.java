package com.eroelf.tfserver.model.handler.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import com.eroelf.tfserver.data.ArrayWrapper;
import com.eroelf.tfserver.data.Sample;
import com.eroelf.tfserver.model.Model;
import com.eroelf.tfserver.model.handler.ModelHandler;

/**
 * Maintains one session of the model in local memory.
 * 
 * @author weikun.zhong
 */
public class LocalModelHandler implements ModelHandler
{
	private ExecutorService es;
	private Model model;
	private int workingNum=0;
	private boolean workingFlag=false;

	private final Object lock=new Object();

	@Override
	public void load(ExecutorService es, String exportDir, Properties properties) throws IOException
	{
		this.es=es;
		String[] tags=properties.getProperty("tags").split("\\|");
		this.model=new Model(exportDir, tags);
		workingFlag=true;
	}

	@Override
	public Map<String, ArrayWrapper> run(Sample sample, Class<? extends ArrayWrapper> outputClass) throws InterruptedException
	{
		synchronized(lock)
		{
			if(!workingFlag)
				return null;
			++workingNum;
		}
		try
		{
			return model.run(sample, outputClass);
		}
		finally
		{
			synchronized(lock)
			{
				--workingNum;
				lock.notify();
			}
		}
	}

	@Override
	public void close() throws IOException
	{
		try
		{
			synchronized(lock)
			{
				while(workingNum!=0)
					lock.wait();
				try
				{
					if(workingFlag)
						workingFlag=false;
					else
						return;
				}
				finally
				{
					lock.notifyAll();
				}
			}
			es.execute(() -> {
				try
				{
					model.close();
				}
				catch(IOException e)
				{
					throw new UncheckedIOException(e);
				}
			});
		}
		catch(InterruptedException e)
		{
			throw new RuntimeException("Close has been interrupted!", e);
		}
	}
}

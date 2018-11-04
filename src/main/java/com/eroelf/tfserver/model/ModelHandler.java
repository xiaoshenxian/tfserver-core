package com.eroelf.tfserver.model;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.eroelf.tfserver.data.ArrayWrapper;
import com.eroelf.tfserver.data.Sample;

/**
 * This class maintains a given number of the same {@link Model} queue in multiple threads.
 * 
 * @author weikun.zhong
 */
public final class ModelHandler implements Closeable
{
	private final ExecutorService es;
	private int workerNum;
	private BlockingQueue<Model> modelQueue;
	private int workingNum=0;
	private boolean workingFlag=false;

	private final Object lock=new Object();

	public ModelHandler(ExecutorService es, int workerNum, String exportDir, String... tags) throws InterruptedException, ExecutionException
	{
		this.es=es;
		this.workerNum=workerNum;
		modelQueue=new ArrayBlockingQueue<>(workerNum);
		List<Future<Model>> list=new ArrayList<>(workerNum);
		for(int i=0; i<workerNum; i++)
		{
			list.add(es.submit(() -> new Model(exportDir, tags)));
		}
		for(Future<Model> future : list)
		{
			modelQueue.put(future.get());
		}
		workingFlag=true;
	}

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
			Model model=modelQueue.take();
			try
			{
				return model.run(sample, outputClass);
			}
			finally
			{
				modelQueue.put(model);
			}
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
			for(int i=0; i<workerNum; i++)
			{
				Model model=modelQueue.take();
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
		}
		catch(InterruptedException e)
		{
			throw new RuntimeException("Close has been interrupted!", e);
		}
	}
}

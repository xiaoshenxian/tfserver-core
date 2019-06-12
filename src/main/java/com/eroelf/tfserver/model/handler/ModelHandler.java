package com.eroelf.tfserver.model.handler;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import com.eroelf.tfserver.data.ArrayWrapper;
import com.eroelf.tfserver.data.Sample;
import com.eroelf.tfserver.model.Model;

/**
 * Instances of this interface maintains sessions of the same {@link Model}.
 * 
 * @author weikun.zhong
 */
public interface ModelHandler extends Closeable
{
	public void load(ExecutorService es, String exportDir, Properties properties) throws IOException;
	public Map<String, ArrayWrapper> run(Sample sample, Class<? extends ArrayWrapper> outputClass) throws InterruptedException;
}

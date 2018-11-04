package com.eroelf.tfserver.data;

import org.tensorflow.framework.DataType;

/**
 * This is an interface to wrap TensorFlow array data type and another data type used for transferring.
 * 
 * @author weikun.zhong
 */
public interface ArrayWrapper
{
	public DataType getType();
	public int[] getShape();
	public <T> T getFeedData(DataType type);
	default public <T> T getFeedData()
	{
		return getFeedData(null);
	}

	public <T> T getWrappedData();

	public void setType(DataType type);
	public void setShape(int[] shape);
	public <T> void setFeedData(T array);

	public <T> void setWrappedData(T obj);
}

package com.eroelf.tfserver.data;

import org.tensorflow.framework.DataType;

/**
 * This class is used to summary {@link ArrayWrapper}s. Only the data type and array shape will be recorded.
 * 
 * @author weikun.zhong
 */
public class ArrayWrapperSummary implements ArrayWrapper
{
	private DataType type;
	private int[] shape;

	public ArrayWrapperSummary(ArrayWrapper arrayWrapper)
	{
		type=arrayWrapper.getType();
		shape=arrayWrapper.getShape();
	}

	@Override
	public DataType getType()
	{
		return type;
	}

	@Override
	public int[] getShape()
	{
		return shape;
	}

	@Override
	public <T> T getFeedData(DataType type)
	{
		throw new UnsupportedOperationException("This method is not allowed!");
	}

	@Override
	public <T> T getWrappedData()
	{
		throw new UnsupportedOperationException("This method is not allowed!");
	}

	@Override
	public void setType(DataType type)
	{
		this.type=type;
	}

	@Override
	public void setShape(int[] shape)
	{
		this.shape=shape;
	}

	@Override
	public <T> void setFeedData(T array)
	{
		throw new UnsupportedOperationException("This method is not allowed!");
	}

	@Override
	public <T> void setWrappedData(T obj)
	{
		throw new UnsupportedOperationException("This method is not allowed!");
	}
}

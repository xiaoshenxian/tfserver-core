package com.eroelf.tfserver.data;

import org.tensorflow.framework.DataType;

import com.eroelf.javaxsx.util.ArrayUtil;

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
		return getFeedData(getType());
	}

	public <T> T getWrappedData(DataType type);
	default public <T> T getWrappedData()
	{
		return getWrappedData(getType());
	}

	public void setType(DataType type);
	public void setShape(int[] shape);

	public <T> void setFeedData(T array);

	public <T> void setWrappedData(T obj);

	default public DataType getArrayElemDataType(Object array)
	{
		if(array==null)
			return DataType.DT_INVALID;

		Class<?> elemClass=ArrayUtil.getComponentType(array);
		if(elemClass==float.class || elemClass==Float.class)
			return DataType.DT_FLOAT;
		else if(elemClass==double.class || elemClass==Double.class)
			return DataType.DT_DOUBLE;
		else if(elemClass==int.class || elemClass==Integer.class)
			return DataType.DT_INT32;
		else if(elemClass==byte.class || elemClass==Byte.class)
			return DataType.DT_UINT8;
		else if(elemClass==String.class)
			return DataType.DT_STRING;
		else if(elemClass==long.class || elemClass==Long.class)
			return DataType.DT_INT64;
		else if(elemClass==boolean.class || elemClass==Boolean.class)
			return DataType.DT_BOOL;
		else
			return DataType.DT_INVALID;
	}
}

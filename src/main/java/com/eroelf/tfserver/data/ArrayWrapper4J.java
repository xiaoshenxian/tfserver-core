package com.eroelf.tfserver.data;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.tensorflow.framework.DataType;

import com.eroelf.javaxsx.util.ArrayUtil;
import com.eroelf.tfserver.exception.DataTypeMismatchException;
import com.eroelf.tfserver.exception.ShapeMismatchExceptioin;
import com.eroelf.tfserver.exception.UnsupportedDataTypeException;

/**
 * Wrap TensorFlow array data type and java {@link List}.
 * 
 * @param <T> the element type. Must corresponds to the {@link DataType} {@code type}.
 * 
 * @author weikun.zhong
 */
public class ArrayWrapper4J<T> implements ArrayWrapper
{
	private DataType type;
	private int[] shape;
	private Object data;

	private transient List<T> listData;

	public ArrayWrapper4J()
	{
		this.type=DataType.DT_INVALID;
	}

	public ArrayWrapper4J(DataType type, Object data)
	{
		if(data!=null && !data.getClass().isArray())
			throw new IllegalArgumentException("data must be an array or keep null!");
		this.type=type;
		this.data=data;
	}

	public ArrayWrapper4J(DataType type, int[] shape, List<T> listData)
	{
		this.type=type;
		this.shape=shape;
		this.listData=listData;
	}

	@Override
	public DataType getType()
	{
		return type;
	}

	@Override
	public int[] getShape()
	{
		if(shape==null && data!=null && data.getClass().isArray())
		{
			List<Integer> li=new ArrayList<>();
			Object obj=data;
			do
			{
				int len=Array.getLength(obj);
				li.add(len);
				if(len>0)
					obj=Array.get(obj, 0);
				else
					break;
			}while(obj.getClass().isArray());
			shape=new int[li.size()];
			int i=0;
			for(int x : li)
			{
				shape[i++]=x;
			}
		}
		return shape;
	}

	@SuppressWarnings({"unchecked"})
	public <U> U getFeedData(DataType type)
	{
		if(data==null)
		{
			int expectedSize=ArrayUtil.getFacets(getShape())[0];
			int dataSize=((List<?>)listData).size();
			if(dataSize!=expectedSize)
				throw new ShapeMismatchExceptioin(String.format("Shape mismatch! Cannot arrange data with size %d to the given shape %s!", dataSize, Arrays.toString(getShape())));
			try
			{
				switch(type!=null ? type : getType())
				{
				case DT_FLOAT:
					data=ArrayUtil.toArray(((List<? extends Number>)listData).stream().map(x -> x.floatValue()).iterator(), getShape(), float.class, (float)0);
					break;
				case DT_DOUBLE:
					data=ArrayUtil.toArray(((List<? extends Number>)listData).stream().mapToDouble(x -> x.doubleValue()).iterator(), getShape(), double.class, (double)0);
					break;
				case DT_INT32:
					data=ArrayUtil.toArray(((List<? extends Number>)listData).stream().mapToInt(x -> x.intValue()).iterator(), getShape(), int.class, (int)0);
					break;
				case DT_UINT8:
					data=ArrayUtil.toArray(((List<? extends Number>)listData).stream().map(x -> x.byteValue()).iterator(), getShape(), byte.class, (byte)0);
					break;
				case DT_STRING:
					data=ArrayUtil.toArray(((List<String>)listData).stream().map(x -> {
						try
						{
							return x.getBytes("utf8");
						}
						catch(UnsupportedEncodingException e)
						{
							throw new Error(e);
						}
					}).iterator(), getShape(), byte[].class, new byte[0]);
					break;
				case DT_INT64:
					data=ArrayUtil.toArray(((List<? extends Number>)listData).stream().mapToLong(x -> x.longValue()).iterator(), getShape(), long.class, (long)0);
					break;
				case DT_BOOL:
					data=ArrayUtil.toArray((List<Boolean>)listData, getShape(), boolean.class, false);
					break;
				default:
					throw new UnsupportedDataTypeException(String.format("Unsupported data type '%s'!", type.name()));
				}
			}
			catch(ClassCastException e)
			{
				throw new DataTypeMismatchException(String.format("Data type mismatch! Expect '%s', and the cause message is: '%s', check the cause for detail!", type.name(), e.getMessage()), e);
			}
		}
		return (U)data;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> getWrappedData()
	{
		if(listData==null)
		{
			listData=new ArrayList<T>();
			Iterator<T> iter;
			if(getType()!=DataType.DT_STRING)
				iter=ArrayUtil.arrayIterator(data, getShape(), 0);
			else
				iter=new Iterator<T>() {
					private Iterator<byte[]> iterator=ArrayUtil.arrayIterator(data, getShape(), 0);

					@Override
					public boolean hasNext()
					{
						return iterator.hasNext();
					}

					@Override
					public T next()
					{
						try
						{
							return (T)new String(iterator.next(), "utf8");
						}
						catch(UnsupportedEncodingException e)
						{
							throw new Error(e);
						}
					}
				};
			while(iter.hasNext())
			{
				listData.add(iter.next());
			}
		}
		return listData;
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
	public <U> void setFeedData(U array)
	{
		data=array;
		listData=null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <U> void setWrappedData(U obj)
	{
		listData=(List<T>)obj;
		data=null;
	}

	@SuppressWarnings("unchecked")
	public void tuneData()
	{
		if(data instanceof List)
		{
			listData=(List<T>)data;
			data=null;
		}
	}
}

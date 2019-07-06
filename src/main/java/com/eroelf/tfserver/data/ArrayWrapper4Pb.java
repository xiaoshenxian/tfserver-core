package com.eroelf.tfserver.data;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.tensorflow.framework.DataType;

import com.eroelf.javaxsx.util.ArrayUtil;
import com.eroelf.tfserver.datastream.BoolArray;
import com.eroelf.tfserver.datastream.DataArray;
import com.eroelf.tfserver.datastream.DoubleArray;
import com.eroelf.tfserver.datastream.FloatArray;
import com.eroelf.tfserver.datastream.Int32Array;
import com.eroelf.tfserver.datastream.Int64Array;
import com.eroelf.tfserver.datastream.StringArray;
import com.eroelf.tfserver.datastream.UInt8Array;
import com.eroelf.tfserver.exception.DataTypeMismatchException;
import com.eroelf.tfserver.exception.ShapeMismatchExceptioin;
import com.eroelf.tfserver.exception.UnsupportedDataTypeException;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Wraps TensorFlow array data type and ProtoBuf {@link DataArray}.
 * 
 * @author weikun.zhong
 */
public class ArrayWrapper4Pb implements ArrayWrapper
{
	private DataType type;
	private int[] shape;
	private Object data;

	private transient DataArray pbData;

	public ArrayWrapper4Pb()
	{
		this.type=DataType.DT_INVALID;
	}

	public ArrayWrapper4Pb(DataType type, Object data)
	{
		if(data!=null && !data.getClass().isArray())
			throw new IllegalArgumentException("data must be an array or keep null!");
		this.type=type;
		this.data=data;
	}

	public ArrayWrapper4Pb(DataArray pbData)
	{
		this.pbData=pbData;
	}

	@Override
	public DataType getType()
	{
		if(type==null)
		{
			if(pbData!=null)
				this.type=DataType.forNumber(pbData.getTypeNum());
			else
				type=getArrayElemDataType(data);
		}
		return type;
	}

	@Override
	public int[] getShape()
	{
		if(shape==null)
		{
			if(pbData!=null)
			{
				List<Integer> list=pbData.getShapeList();
				shape=new int[list.size()];
				int i=0;
				for(int x : list)
				{
					shape[i++]=x;
				}
			}
			else if(data!=null)
				shape=ArrayUtil.getShape(data);
		}
		return shape;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <U> U getFeedData(DataType type)
	{
		if(data==null)
		{
			int expectedSize=ArrayUtil.getFacets(getShape())[0];
			int dataSize;
			switch(type!=null ? type : getType())
			{
			case DT_FLOAT:
				try
				{
					FloatArray array=pbData.getData().unpack(FloatArray.class);
					dataSize=array.getDataCount();
					if(dataSize==expectedSize)
						data=ArrayUtil.toArray(array.getDataList(), getShape(), float.class, (float)0);
					else
						throw new ShapeMismatchExceptioin(String.format("Shape mismatch! Cannot arrange data with size %d to the given shape %s!", dataSize, Arrays.toString(getShape())));
				}
				catch(InvalidProtocolBufferException e)
				{
					throw new DataTypeMismatchException(String.format("Data type mismatch! Expect 'FloatArray' for '%s', recieved '%s'!", type.name(), pbData.getData().getTypeUrl()), e);
				}
				break;
			case DT_DOUBLE:
				try
				{
					DoubleArray array=pbData.getData().unpack(DoubleArray.class);
					dataSize=array.getDataCount();
					if(dataSize==expectedSize)
						data=ArrayUtil.toArray(array.getDataList(), getShape(), double.class, (double)0);
					else
						throw new ShapeMismatchExceptioin(String.format("Shape mismatch! Cannot arrange data with size %d to the given shape %s!", dataSize, Arrays.toString(getShape())));
				}
				catch(InvalidProtocolBufferException e)
				{
					throw new DataTypeMismatchException(String.format("Data type mismatch! Expect 'DoubleArray' for '%s', recieved '%s'!", type.name(), pbData.getData().getTypeUrl()), e);
				}
				break;
			case DT_INT32:
				try
				{
					Int32Array array=pbData.getData().unpack(Int32Array.class);
					dataSize=array.getDataCount();
					if(dataSize==expectedSize)
						data=ArrayUtil.toArray(array.getDataList(), getShape(), int.class, (int)0);
					else
						throw new ShapeMismatchExceptioin(String.format("Shape mismatch! Cannot arrange data with size %d to the given shape %s!", dataSize, Arrays.toString(getShape())));
				}
				catch(InvalidProtocolBufferException e)
				{
					throw new DataTypeMismatchException(String.format("Data type mismatch! Expect 'Int32Array' for '%s', recieved '%s'!", type.name(), pbData.getData().getTypeUrl()), e);
				}
				break;
			case DT_UINT8:
				try
				{
					ByteString array=pbData.getData().unpack(UInt8Array.class).getData();
					dataSize=array.size();
					if(dataSize==expectedSize)
						data=ArrayUtil.toArray(array.iterator(), getShape(), byte.class, (byte)0);
					else
						throw new ShapeMismatchExceptioin(String.format("Shape mismatch! Cannot arrange data with size %d to the given shape %s!", dataSize, Arrays.toString(getShape())));
				}
				catch(InvalidProtocolBufferException e)
				{
					throw new DataTypeMismatchException(String.format("Data type mismatch! Expect 'UInt8Array' for '%s', recieved '%s'!", type.name(), pbData.getData().getTypeUrl()), e);
				}
				break;
			case DT_STRING:
				try
				{
					StringArray array=pbData.getData().unpack(StringArray.class);
					dataSize=array.getDataCount();
					if(dataSize==expectedSize)
						data=ArrayUtil.toArray(array.getDataList().stream().map(x -> {
							try
							{
								return x.getBytes("utf8");
							}
							catch(UnsupportedEncodingException e)
							{
								throw new Error(e);
							}
						}).iterator(), getShape(), byte[].class, new byte[0]);
					else
						throw new ShapeMismatchExceptioin(String.format("Shape mismatch! Cannot arrange data with size %d to the given shape %s!", dataSize, Arrays.toString(getShape())));
				}
				catch(InvalidProtocolBufferException e)
				{
					throw new DataTypeMismatchException(String.format("Data type mismatch! Expect 'StringArray' for '%s', recieved '%s'!", type.name(), pbData.getData().getTypeUrl()), e);
				}
				break;
			case DT_INT64:
				try
				{
					Int64Array array=pbData.getData().unpack(Int64Array.class);
					dataSize=array.getDataCount();
					if(dataSize==expectedSize)
						data=ArrayUtil.toArray(array.getDataList(), getShape(), long.class, (long)0);
					else
						throw new ShapeMismatchExceptioin(String.format("Shape mismatch! Cannot arrange data with size %d to the given shape %s!", dataSize, Arrays.toString(getShape())));
				}
				catch(InvalidProtocolBufferException e)
				{
					throw new DataTypeMismatchException(String.format("Data type mismatch! Expect 'Int64Array' for '%s', recieved '%s'!", type.name(), pbData.getData().getTypeUrl()), e);
				}
				break;
			case DT_BOOL:
				try
				{
					BoolArray array=pbData.getData().unpack(BoolArray.class);
					dataSize=array.getDataCount();
					if(dataSize==expectedSize)
						data=ArrayUtil.toArray(array.getDataList(), getShape(), boolean.class, false);
					else
						throw new ShapeMismatchExceptioin(String.format("Shape mismatch! Cannot arrange data with size %d to the given shape %s!", dataSize, Arrays.toString(getShape())));
				}
				catch(InvalidProtocolBufferException e)
				{
					throw new DataTypeMismatchException(String.format("Data type mismatch! Expect 'BoolArray' for '%s', recieved '%s'!", type.name(), pbData.getData().getTypeUrl()), e);
				}
				break;
			default:
				throw new UnsupportedDataTypeException(String.format("Unsupported data type '%s'!", type.name()));
			}
		}
		return (U)data;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataArray getWrappedData(DataType type)
	{
		if(pbData==null)
		{
			type=type!=null ? type : getType();
			DataArray.Builder builder=DataArray.newBuilder();
			builder.setTypeNum(type.getNumber());
			for(int x : shape)
			{
				builder.addShape(x);
			}
			switch(type)
			{
			case DT_FLOAT:
				pbData=builder.setData(Any.pack(FloatArray.newBuilder().addAllData(new Iterable<Float>() {
					@Override
					public Iterator<Float> iterator()
					{
						return ArrayUtil.arrayIterator(data, shape, 0);
					}
				}).build())).build();
				break;
			case DT_DOUBLE:
				pbData=builder.setData(Any.pack(DoubleArray.newBuilder().addAllData(new Iterable<Double>() {
					@Override
					public Iterator<Double> iterator()
					{
						return ArrayUtil.arrayIterator(data, shape, 0);
					}
				}).build())).build();
				break;
			case DT_INT32:
				pbData=builder.setData(Any.pack(Int32Array.newBuilder().addAllData(new Iterable<Integer>() {
					@Override
					public Iterator<Integer> iterator()
					{
						return ArrayUtil.arrayIterator(data, shape, 0);
					}
				}).build())).build();
				break;
			case DT_UINT8:
				pbData=builder.setData(Any.pack(UInt8Array.newBuilder().setData(ByteString.copyFrom((byte[])ArrayUtil.toArray(ArrayUtil.arrayIterator(data, shape, 0), shape, byte.class, ""))).build())).build();
				break;
			case DT_STRING:
				pbData=builder.setData(Any.pack(StringArray.newBuilder().addAllData(new Iterable<String>() {
					@Override
					public Iterator<String> iterator()
					{
						Stream<byte[]> stream=StreamSupport.stream(Spliterators.spliteratorUnknownSize(ArrayUtil.arrayIterator(data, shape, 0), Spliterator.ORDERED|Spliterator.NONNULL), false);
						return stream.map(x -> {
							try
							{
								return new String(x, "utf8");
							}
							catch(UnsupportedEncodingException e)
							{
								throw new Error(e);
							}
						}).iterator();
					}
				}).build())).build();
				break;
			case DT_INT64:
				pbData=builder.setData(Any.pack(Int64Array.newBuilder().addAllData(new Iterable<Long>() {
					@Override
					public Iterator<Long> iterator()
					{
						return ArrayUtil.arrayIterator(data, shape, 0);
					}
				}).build())).build();
				break;
			case DT_BOOL:
				pbData=builder.setData(Any.pack(BoolArray.newBuilder().addAllData(new Iterable<Boolean>() {
					@Override
					public Iterator<Boolean> iterator()
					{
						return ArrayUtil.arrayIterator(data, shape, 0);
					}
				}).build())).build();
				break;
			default:
				throw new UnsupportedDataTypeException(String.format("Unsupported data type '%s'!", type.name()));
			}
		}
		return pbData;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataArray getWrappedData()
	{
		return getWrappedData(getType());
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
		if(array!=null && !array.getClass().isArray())
			throw new IllegalArgumentException("data must be an array or keep null!");
		data=array;
		pbData=null;
	}

	@Override
	public <T> void setWrappedData(T obj)
	{
		pbData=(DataArray)obj;
		data=null;
	}
}

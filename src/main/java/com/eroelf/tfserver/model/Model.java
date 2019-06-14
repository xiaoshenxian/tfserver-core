package com.eroelf.tfserver.model;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Session.Runner;
import org.tensorflow.Tensor;
import org.tensorflow.framework.DataType;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;

import com.eroelf.javaxsx.util.ArrayUtil;
import com.eroelf.javaxsx.util.Strings;
import com.eroelf.tfserver.data.ArrayWrapper;
import com.eroelf.tfserver.data.Sample;
import com.eroelf.tfserver.exception.UnregisteredInputException;
import com.eroelf.tfserver.exception.UnregisteredSignatureException;
import com.eroelf.tfserver.exception.UnsupportedDataTypeException;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * This class wraps a TensorFlow {@link SavedModelBundle}, and provide methods to load and run it.
 * 
 * @author weikun.zhong
 */
public final class Model implements AutoCloseable
{
	private static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY="serving_default";

	public static class EntityInfo
	{
		public String name;
		public String tensorName;
		public DataType dType; 

		public EntityInfo(String name, TensorInfo tensorInfo)
		{
			this.name=name;
			tensorName=tensorInfo.getName();
			dType=tensorInfo.getDtype();
		}
	}

	public static class Signature
	{
		public SignatureDef sig;
		public Map<String, EntityInfo> inputMap;
		public List<EntityInfo> outputs;

		public Signature(SignatureDef sig)
		{
			this.sig=sig;
			Map<String, TensorInfo> map=sig.getInputsMap();
			inputMap=new HashMap<>(map.size());
			for(Entry<String, TensorInfo> entry : map.entrySet())
			{
				inputMap.put(entry.getKey(), new EntityInfo(entry.getKey(), entry.getValue()));
			}
			map=sig.getOutputsMap();
			outputs=new ArrayList<>(map.size());
			for(Entry<String, TensorInfo> entry : map.entrySet())
			{
				outputs.add(new EntityInfo(entry.getKey(), entry.getValue()));
			}
		}
	}

	private SavedModelBundle bundle;
	private Map<String, Signature> signatureMap=new HashMap<>();

	public Model(String exportDir, String... tags) throws InvalidProtocolBufferException
	{
		bundle=SavedModelBundle.load(exportDir, tags);
		for(Entry<String, SignatureDef> entry : MetaGraphDef.parseFrom(bundle.metaGraphDef()).getSignatureDefMap().entrySet())
		{
			signatureMap.put(entry.getKey(), new Signature(entry.getValue()));
		}
	}

	public Map<String, ArrayWrapper> run(Sample sample, Class<? extends ArrayWrapper> outputClass)
	{
		Signature sig;
		if(Strings.isValid(sample.getSignatureName()))
			sig=signatureMap.get(sample.getSignatureName());
		else
			sig=signatureMap.get(DEFAULT_SERVING_SIGNATURE_DEF_KEY);
		if(sig==null)
			throw new UnregisteredSignatureException(String.format("Unregistered signature: %s. Must in %s, or keep empty for default.", sample.getSignatureName(), signatureMap.keySet().toString()));

		Session sess=bundle.session();
		Runner runner=sess.runner();
		List<Tensor<?>> inputs=new ArrayList<>(sample.getInputs().size());
		List<Tensor<?>> resultTensors;
		Iterator<EntityInfo> outputsIter;
		try
		{
			for(Entry<String, ArrayWrapper> entry : sample.getInputs().entrySet())
			{
				EntityInfo info=sig.inputMap.get(entry.getKey());
				if(info==null)
					throw new UnregisteredInputException(String.format("Unregistered input: %s. Must in %s.", entry.getKey(), sig.inputMap.keySet().toString()));
				Tensor<?> tensor=Tensor.create(entry.getValue().getFeedData(info.dType));
				inputs.add(tensor);
				runner.feed(info.tensorName, tensor);
			}
			outputsIter=sig.outputs.iterator();
			while(outputsIter.hasNext())
			{
				runner.fetch(outputsIter.next().tensorName);
			}
			resultTensors=runner.run();
		}
		finally
		{
			for(Tensor<?> tensor : inputs)
			{
				tensor.close();
			}
		}

		Map<String, ArrayWrapper> results=new HashMap<>();
		outputsIter=sig.outputs.iterator();
		Iterator<Tensor<?>> resultIter=resultTensors.iterator();
		while(outputsIter.hasNext() && resultIter.hasNext())
		{
			EntityInfo info=outputsIter.next();
			Tensor<?> tensor=resultIter.next();
			int[] shape;
			Object res;
			try
			{
				long[] shapeLong=tensor.shape();
				shape=new int[shapeLong.length];
				for(int i=0; i<shape.length; i++)
				{
					shape[i]=(int)shapeLong[i];
				}
				switch(info.dType)
				{
				case DT_FLOAT:
					res=ArrayUtil.newArray(shape, float.class);
					break;
				case DT_DOUBLE:
					res=ArrayUtil.newArray(shape, double.class);
					break;
				case DT_INT32:
					res=ArrayUtil.newArray(shape, int.class);
					break;
				case DT_UINT8:
					res=ArrayUtil.newArray(shape, byte.class);
					break;
				case DT_STRING:
					res=ArrayUtil.newArray(shape, byte[].class);
					break;
				case DT_INT64:
					res=ArrayUtil.newArray(shape, long.class);
					break;
				case DT_BOOL:
					res=ArrayUtil.newArray(shape, boolean.class);
					break;
				default:
					throw new UnsupportedDataTypeException(String.format("Unsupported data type! '%s'!", info.dType.name()));
				}
				tensor.copyTo(res);
			}
			finally
			{
				tensor.close();
			}

			ArrayWrapper output;
			try
			{
				output=outputClass.newInstance();
				if(info.dType==DataType.DT_STRING)
				{
					Object res1=ArrayUtil.toArray(new Iterator<String>() {
						private Iterator<byte[]> iter=ArrayUtil.arrayIterator(res, shape, 0);

						@Override
						public String next()
						{
							try
							{
								return new String(iter.next(), "utf8");
							}
							catch(UnsupportedEncodingException e)
							{
								throw new Error(e);
							}
						}

						@Override
						public boolean hasNext()
						{
							return iter.hasNext();
						}
					}, shape, String.class, "");
					output.setFeedData(res1);
				}
				else
					output.setFeedData(res);
				output.setType(info.dType);
				output.setShape(shape);
				results.put(info.name, output);
			}
			catch(InstantiationException|IllegalAccessException e)
			{
				throw new Error(e);
			}
		}
		return results;
	}

	@Override
	public void close() throws IOException
	{
		bundle.close();
	}
}

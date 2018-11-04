package com.eroelf.tfserver.data;

import java.util.Map;
import java.util.stream.Collectors;

import org.tensorflow.framework.DataType;

/**
 * A TensorFlow model sample with data wrapped by {@link ArrayWrapper4Pb} and saved in a {@link com.eroelf.tfserver.datastream.Sample} object.
 * 
 * @author weikun.zhong
 */
public class Sample4Pb implements Sample
{
	private com.eroelf.tfserver.datastream.Sample pbSample;

	public Sample4Pb(com.eroelf.tfserver.datastream.Sample pbSample)
	{
		this.pbSample=pbSample;
	}

	@Override
	public String getModelName()
	{
		return pbSample.getModelName();
	}

	@Override
	public String getModelVersion()
	{
		return pbSample.getModelVersion();
	}

	@Override
	public String getSignatureName()
	{
		return pbSample.getSignatureName();
	}

	@Override
	public Map<String, ArrayWrapper> getInputs()
	{
		return pbSample.getInputsMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> new ArrayWrapper4Pb(DataType.DT_INVALID, e.getValue())));
	}
}

package com.eroelf.tfserver.data;

import java.util.HashMap;
import java.util.Map;

/**
 * A TensorFlow model sample with data wrapped by {@link ArrayWrapper4J}.
 * 
 * @author weikun.zhong
 */
public class Sample4J implements Sample
{
	public String modelName;
	public String modelVersion;
	public String signatureName;
	public Map<String, ArrayWrapper4J<?>> inputs;

	@Override
	public String getModelName()
	{
		return modelName;
	}

	@Override
	public String getModelVersion()
	{
		return modelVersion;
	}

	@Override
	public String getSignatureName()
	{
		return signatureName;
	}

	@Override
	public Map<String, ArrayWrapper> getInputs()
	{
		return new HashMap<>(inputs);
	}
}

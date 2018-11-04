package com.eroelf.tfserver.data;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used to summary {@link Sample}s.
 * 
 * @author weikun.zhong
 */
public class SampleSummary implements Sample
{
	public String modelName;
	public String modelVersion;
	public String signatureName;
	public Map<String, ArrayWrapperSummary> inputs;

	public SampleSummary(Sample sample)
	{
		modelName=sample.getModelName();
		modelVersion=sample.getModelVersion();
		signatureName=sample.getSignatureName();
		inputs=sample.getInputs().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> new ArrayWrapperSummary(e.getValue())));
	}

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

package com.eroelf.tfserver.data;

import java.util.Map;

/**
 * This is an interface provides all information for an exported TensorFlow model to run.
 * 
 * @author weikun.zhong
 */
public interface Sample
{
	public String getModelName();
	public String getModelVersion();
	public String getSignatureName();
	public Map<String, ArrayWrapper> getInputs();
}

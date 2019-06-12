package com.eroelf.tfserver.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * Helps to control models and requests for a TensorFlow model server.
 * 
 * @author weikun.zhong
 */
public class ModelHelper
{
	public static Properties loadConfig(String dirName, String fileName) throws FileNotFoundException, IOException
	{
		Properties properties=new Properties();
		properties.load(new FileInputStream(Paths.get(dirName, fileName).toFile()));
		return properties;
	}

	public static Properties loadConfigQuiet(String dirName, String fileName)
	{
		Properties properties=new Properties();
		try
		{
			properties.load(new FileInputStream(Paths.get(dirName, fileName).toFile()));
		}
		catch(IOException e)
		{}
		return properties;
	}

	public static Properties complementFromDefault(Properties properties, Properties defaultProperties)
	{
		for(Entry<Object, Object> entry : defaultProperties.entrySet())
		{
			properties.putIfAbsent(entry.getKey(), entry.getValue());
		}
		return properties;
	}
}

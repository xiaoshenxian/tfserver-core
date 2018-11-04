package com.eroelf.tfserver.exception;

/**
 * Thrown to indicate that the passed data type has not been supported.
 * 
 * @author weikun.zhong
 */
public class UnsupportedDataTypeException extends WorkingFlowException
{
	private static final long serialVersionUID=-7717528265867227519L;

	@Override
	public int getCode()
	{
		return 20;
	}

	public UnsupportedDataTypeException()
	{
		super();
	}
	public UnsupportedDataTypeException(String s)
	{
		super(s);
	}

	public UnsupportedDataTypeException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public UnsupportedDataTypeException(Throwable cause)
	{
		super(cause);
	}
}

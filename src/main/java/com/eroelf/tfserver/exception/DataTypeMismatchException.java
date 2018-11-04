package com.eroelf.tfserver.exception;

/**
 * Thrown to indicate that the passed data type has not matched the recorded one.
 * 
 * @author weikun.zhong
 */
public class DataTypeMismatchException extends WorkingFlowException
{
	private static final long serialVersionUID=-1467761678874981405L;

	@Override
	public int getCode()
	{
		return 21;
	}

	public DataTypeMismatchException()
	{
		super();
	}
	public DataTypeMismatchException(String s)
	{
		super(s);
	}

	public DataTypeMismatchException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public DataTypeMismatchException(Throwable cause)
	{
		super(cause);
	}
}

package com.eroelf.tfserver.exception;

/**
 * Thrown to indicate that the specified model has not been registered to the TensorFlow server.
 * 
 * @author weikun.zhong
 */
public class UnregisteredModelException extends WorkingFlowException
{
	private static final long serialVersionUID=5346642775990500579L;

	@Override
	public int getCode()
	{
		return 10;
	}

	public UnregisteredModelException()
	{
		super();
	}
	public UnregisteredModelException(String s)
	{
		super(s);
	}

	public UnregisteredModelException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public UnregisteredModelException(Throwable cause)
	{
		super(cause);
	}
}

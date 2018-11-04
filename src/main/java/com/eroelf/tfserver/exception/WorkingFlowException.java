package com.eroelf.tfserver.exception;

/**
 * Thrown to indicate that a request parameter has broken the working flow.
 * 
 * @author weikun.zhong
 */
public abstract class WorkingFlowException extends IllegalArgumentException
{
	private static final long serialVersionUID=-4126102957143637668L;

	public WorkingFlowException()
	{
		super();
	}
	public WorkingFlowException(String s)
	{
		super(s);
	}

	public WorkingFlowException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public WorkingFlowException(Throwable cause)
	{
		super(cause);
	}

	public abstract int getCode();
}

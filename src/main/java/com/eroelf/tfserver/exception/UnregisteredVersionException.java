package com.eroelf.tfserver.exception;

/**
 * Thrown to indicate that the specified model has not had the passed version.
 * 
 * @author weikun.zhong
 */
public class UnregisteredVersionException extends WorkingFlowException
{
	private static final long serialVersionUID=2313488057224660127L;

	@Override
	public int getCode()
	{
		return 11;
	}

	public UnregisteredVersionException()
	{
		super();
	}
	public UnregisteredVersionException(String s)
	{
		super(s);
	}

	public UnregisteredVersionException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public UnregisteredVersionException(Throwable cause)
	{
		super(cause);
	}
}

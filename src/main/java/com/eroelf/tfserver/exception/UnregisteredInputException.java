package com.eroelf.tfserver.exception;

/**
 * Thrown to indicate that the specified model do not has the passed input.
 * 
 * @author weikun.zhong
 */
public class UnregisteredInputException extends WorkingFlowException
{
	private static final long serialVersionUID=8712824060183782843L;

	@Override
	public int getCode()
	{
		return 13;
	}

	public UnregisteredInputException()
	{
		super();
	}
	public UnregisteredInputException(String s)
	{
		super(s);
	}

	public UnregisteredInputException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public UnregisteredInputException(Throwable cause)
	{
		super(cause);
	}
}

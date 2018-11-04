package com.eroelf.tfserver.exception;

/**
 * Thrown to indicate that the specified model has not had the passed signature.
 * 
 * @author weikun.zhong
 */
public class UnregisteredSignatureException extends WorkingFlowException
{
	private static final long serialVersionUID=5057948562950035229L;

	@Override
	public int getCode()
	{
		return 12;
	}

	public UnregisteredSignatureException()
	{
		super();
	}
	public UnregisteredSignatureException(String s)
	{
		super(s);
	}

	public UnregisteredSignatureException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public UnregisteredSignatureException(Throwable cause)
	{
		super(cause);
	}
}

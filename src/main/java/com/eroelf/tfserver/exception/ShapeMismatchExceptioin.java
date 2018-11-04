package com.eroelf.tfserver.exception;

/**
 * Thrown to indicate that a wrong shape has been passed.
 * 
 * @author weikun.zhong
 */
public class ShapeMismatchExceptioin extends WorkingFlowException
{
	private static final long serialVersionUID=3955755841539184328L;

	@Override
	public int getCode()
	{
		return 30;
	}

	public ShapeMismatchExceptioin()
	{
		super();
	}
	public ShapeMismatchExceptioin(String s)
	{
		super(s);
	}

	public ShapeMismatchExceptioin(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ShapeMismatchExceptioin(Throwable cause)
	{
		super(cause);
	}
}

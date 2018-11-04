package com.eroelf.tfserver.exception;

/**
 * Thrown to indicate that an invalid response has been received.
 * 
 * @author weikun.zhong
 */
public class ResponseException extends RuntimeException
{
	private static final long serialVersionUID=2932973827750043022L;

	public ResponseException()
	{
		super();
	}
	public ResponseException(String s)
	{
		super(s);
	}

	public ResponseException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ResponseException(Throwable cause)
	{
		super(cause);
	}
}

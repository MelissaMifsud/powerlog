package net.melissam.powerlog.utils;

public class PowerLogException extends Exception {

	/** Serial version UUID. */
	private static final long serialVersionUID = 5832941417989019320L;
	
	public PowerLogException(String message){
		super(message);
	}
	
	public PowerLogException(String message, Throwable cause){
		super(message, cause);
	}
}

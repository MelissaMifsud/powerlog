package net.melissam.powerlog.utils;

public class InvalidLogMessage extends PowerLogException {

	/** Serial version UID. */
	private static final long serialVersionUID = 8558847804692155008L;

	public InvalidLogMessage(String message){
		super(message);
	}
	
	public InvalidLogMessage(String message, Throwable cause){
		super(message, cause);
	}
	
}

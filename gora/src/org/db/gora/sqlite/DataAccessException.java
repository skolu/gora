package org.db.gora.sqlite;

public class DataAccessException extends Exception {
	public DataAccessException(String detailedMessage) {
		super(detailedMessage);
	}
	
	public DataAccessException(String detailedMessage, Throwable throwable) {
		super(detailedMessage, throwable);
	}

	private static final long serialVersionUID = -8507999681855900760L;
}

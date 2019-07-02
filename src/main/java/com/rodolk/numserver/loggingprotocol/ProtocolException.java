package com.rodolk.numserver.loggingprotocol;

public class ProtocolException extends Exception {
	public enum ErrorCode {READER_ERROR, SOCKET_ERROR, CLOSED, END}
	private ErrorCode error_;
	
	public ProtocolException(String message, ErrorCode err) {
		super(message);
		error_ = err;
	}
	
	public ErrorCode getError() {
		return error_;
	}

}

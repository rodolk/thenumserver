package com.rodolk.numserver.loggingprotocol;

/**
 * ProtocolException is used during the processing of the application protocol to indicate:
 * <ul>
 * <li>READER_ERROR: if data received doesn't comply with the expected format or there is an
 *                   error reading data.
 * <li>SOCKET_ERROR: When there was a socket error like a read timeout.
 * <li>CLOSED:       When the client closes the connection.
 * <li>END:          When the protocol reached the end of end state.
 * </ul>
 * <p>
 * 
 * @author rodolk
 *
 */
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

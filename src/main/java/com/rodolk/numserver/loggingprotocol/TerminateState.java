package com.rodolk.numserver.loggingprotocol;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class TerminateState extends State {

	char[] charArray_;
	int nextPos_;
	char[] terminateArray_ = {'t', 'e', 'r', 'm', 'i', 'n', 'a', 't', 'e', '\n'};
	boolean error_ = false;
	
	public TerminateState(ApplicationProtocol prot, char[] charArray, int currentPos, int lastPos) {
		super(prot);
		
		charArray_ = new char[(protocol_.kIntLen + 1)];
		int i;
		for(i = 0; i < (lastPos - currentPos + 1) && i < (protocol_.kIntLen + 1); i++) {
			charArray_[i] = charArray[currentPos + i];
			if (terminateArray_[i] != charArray_[i]) {
				error_ = true;
			}
		}
		nextPos_ = i;
	}
	
	@Override
	public State process() throws ProtocolException {
		int len;
		
		if (error_ == true) {
			throw new ProtocolException("Erroneous data", ProtocolException.ErrorCode.READER_ERROR);
		}
		
		if (nextPos_ == (protocol_.kIntLen + 1)) {
			throw new ProtocolException("End", ProtocolException.ErrorCode.END);
		}
		
		try {
			len = protocol_.inBuffReader_.read(charArray_, nextPos_, (protocol_.kIntLen + 1) - nextPos_);
		} catch (SocketTimeoutException e1) {
			e1.printStackTrace();
			throw new ProtocolException("Socket timeout", ProtocolException.ErrorCode.SOCKET_ERROR);
		} catch (IOException e2) {
			e2.printStackTrace();
			throw new ProtocolException("Error reading", ProtocolException.ErrorCode.READER_ERROR);
		}
		
		if (len == -1) {
			try {
				protocol_.inBuffReader_.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			throw new ProtocolException("Closed", ProtocolException.ErrorCode.CLOSED);
		} else {
			int i;
			for(i = nextPos_; i < nextPos_ + len; i++) {
				if (terminateArray_[i] != charArray_[i]) {
					throw new ProtocolException("Erroneous data", ProtocolException.ErrorCode.READER_ERROR);
				} if (i == (protocol_.kIntLen + 1)) {
					throw new ProtocolException("End", ProtocolException.ErrorCode.END);
				}
			}
			nextPos_ = i;
		}
		return this;
	}
}

package com.rodolk.numserver.loggingprotocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IdleState extends State {
	
	public IdleState(ApplicationProtocol prot) {
		super(prot);
	}
	
	public State process() throws ProtocolException {
		try {
			protocol_.inBuffReader_ = new BufferedReader(new InputStreamReader(protocol_.socket_.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
			throw new ProtocolException("Error creating BufferedReader", 
					ProtocolException.ErrorCode.READER_ERROR);
		}

		return new ConnectedState(protocol_);
	}
}

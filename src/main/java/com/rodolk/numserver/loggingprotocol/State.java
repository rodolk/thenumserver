package com.rodolk.numserver.loggingprotocol;

public abstract class State {
	protected ApplicationProtocol protocol_ = null;
	
	public State(ApplicationProtocol prot) {
		protocol_ = prot;
	}
	
	public abstract State process() throws ProtocolException ;
}

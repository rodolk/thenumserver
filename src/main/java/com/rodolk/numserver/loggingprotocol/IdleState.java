package com.rodolk.numserver.loggingprotocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * IdleState is the initial state and when process is called, 
 * it creates the BufferedReader to read from the socket. Then it moves to ConnectedState.
 * 
 * @author rodolk
 *
 */
public class IdleState extends State {
	
	public IdleState(ApplicationProtocol prot) {
		super(prot);
	}
	
	/**
	 * Creates the BufferedReader to read from socket_
	 * Then it transitions to ConnectedState: it creates a new ConnectedState and returns it
	 * to be used as the next State.
	 * 
	 * @return	<code>State</code> A ConnectedState
	 * 
	 * @throws ProtcolException If there is an error in the creation of the BufferedReader
	 * 
	 */
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

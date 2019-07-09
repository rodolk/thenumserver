package com.rodolk.numserver.loggingprotocol;

/**
 * State is an abstract class from which all state classes inherit.
 * All children must implement process method.
 * 
 * @author rodolk
 *
 */
public abstract class State {
    protected ApplicationProtocol protocol_ = null;
    
    public State(ApplicationProtocol prot) {
        protocol_ = prot;
    }
    
    /**
     * All children of State must implement this method. The protocol manager must call
     * process to process data and transition through different states of the protocol. 
     * 
     * @return    <code>State</code> Next state
     * 
     * @throws ProtocolException If there is an error in the data received (READER_ERROR), 
     *                          there is a socket error (SOCKET_ERROR), or connection is closed 
     *                          by client (CLOSED), or final state completed (END).
     * 
     */
    public abstract State process() throws ProtocolException ;
}

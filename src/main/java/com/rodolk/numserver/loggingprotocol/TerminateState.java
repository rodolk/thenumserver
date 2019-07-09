package com.rodolk.numserver.loggingprotocol;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * TerminateState is trying to read the complete 'terminate\n' string.
 * This is the final state of the protocol when there is no error or connection
 * is not closed.
 * 
 * @author rodolk
 *
 */
public class TerminateState extends State {

    char[] charArray_;
    int nextPos_;
    char[] terminateArray_ = null;
    boolean error_ = false;
    int newLineLen_;
    char[] newLineCharArray_;
    
    public TerminateState(ApplicationProtocol prot, char[] charArray, int currentPos, int lastPos) {
        super(prot);
        
        newLineLen_ = protocol_.getNewLineLen();
        newLineCharArray_ = protocol_.getNewLine();
        
        terminateArray_ = new char[protocol_.kTerminateString.length() + newLineLen_];
        for(int i = 0; i < protocol_.kTerminateString.length(); i++) {
            terminateArray_[i] = protocol_.kTerminateString.charAt(i);
        }
        
        for(int i = 0; i < newLineLen_; i++) {
            terminateArray_[protocol_.kTerminateString.length() + i] = newLineCharArray_[i];
        }

        charArray_ = new char[(protocol_.kIntLen + newLineLen_)];
        int i;
        for(i = 0; i < (lastPos - currentPos + 1) && i < (protocol_.kIntLen + newLineLen_); i++) {
            charArray_[i] = charArray[currentPos + i];
            if (terminateArray_[i] != charArray_[i]) {
                error_ = true;
            }
        }
        nextPos_ = i;
    }
    
    /**
     * Reads data sent by client, until all letters from 'termiante\n' are received.
     * When the complete string is received including new line, a protocol exception
     * with error code ProtocolException.ErrorCode.END is thrown.
     * 
     * @return    <code>State</code> TerminateState if it did not receive all letters yet.
     * 
     * @throws ProtocolException If there is an error in the data received (READER_ERROR), 
     *                          there is a socket error (SOCKET_ERROR), or connection is closed 
     *                          by client (CLOSED), or all letters are received (END)
     * 
     */
    @Override
    public State process() throws ProtocolException {
        int len;
        
        if (error_ == true) {
            throw new ProtocolException("Erroneous data", ProtocolException.ErrorCode.READER_ERROR);
        }
        
        if (nextPos_ == (protocol_.kIntLen + newLineLen_)) {
            throw new ProtocolException("End", ProtocolException.ErrorCode.END);
        }
        
        try {
            len = protocol_.inBuffReader_.read(charArray_, nextPos_, (protocol_.kIntLen + newLineLen_) - nextPos_);
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
                } if (i == (protocol_.kIntLen + newLineLen_)) {
                    throw new ProtocolException("End", ProtocolException.ErrorCode.END);
                }
            }
            nextPos_ = i;
        }
        return this;
    }
}

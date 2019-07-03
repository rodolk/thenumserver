package com.rodolk.numserver.loggingprotocol;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * ConnectedState is the main state where the protocol is reading data sent by the client.
 * It parses the data and checks correctness. 
 * Then it calls <code>protocol_.dataConsumer_.processData<\code> to process the 
 * received and checked data.
 * If 'terminate\n' is received from the client it will transition to TerminateState.
 * 
 * @author rodolk
 *
 */
public class ConnectedState extends State {

    ArrayProvider.ArrayElement arrayElem_;
    private int nextOffset_ = 0;
    private static char[] newLineChars_ = null; 
    private int newLineLen_ = 0; 
    private int nextNewLineIdx_ = 0;

    public ConnectedState(ApplicationProtocol prot) {
        super(prot);
        newLineLen_ = protocol_.getNewLineLen();
        newLineChars_ = protocol_.getNewLine();
        arrayElem_ = protocol_.arrayProvider_.getArray();
    }
    
    /**
     * Reads data sent by client, parses it and checks correctness. It tries to read a complete 
     * buffer but it could read less than a buffer. The buffer size is 2 typical MTUs of 1500 - 40
     * to be able to read up to two full TCP messages for a client with an increased congestion window.
     * However we don't want too big buffer to avoid external fragmentation and waste of memory.
     * An optimization done here is that if too few characters were received, this method reads again 
     * from the socket with a smaller timeout of 200ms. This should be enough for most RTTs in North America
     * and Europe. This is done to try to read into the buffer and process as much data as possible.
     * Because of the stream nature of TCP, we need to consider a number could be received partially. 
     * In this case the partial number is copied to initial part of the next buffer.
     * We need to set a read timeout for the socket for a case of network fragmentation or a client
     * just stopping sending data.
     * If the letter 't' is received as first character after '\n', then the protocol transitions to
     * TermianteState.
     * 
     * @return    <code>State</code> ConnectedState if still reading data
     *                             TerminateState if it received 'terminate\n'
     * 
     * @throws ProtcolException If there is an error in the data received (READER_ERROR), 
     *                          there is a socket error (SOCKET_ERROR), or connection is closed 
     *                          by client (CLOSED)
     * 
     */
    @Override
    public State process() throws ProtocolException {
        int len = 0;
        State nextState = this;
        nextNewLineIdx_ = 0;
        
        try {
            len = protocol_.inBuffReader_.read(arrayElem_.array_, nextOffset_, protocol_.kArrayLen - nextOffset_);
            len += nextOffset_;
            if (len != -1 && len < protocol_.kMinThreshold) {
                //If read a small amount of data, try to read as much data as possible in a buffer
                protocol_.socket_.setSoTimeout(200); //Should be enough for a big RTT

                try {
                    int len2 = protocol_.inBuffReader_.read(arrayElem_.array_, len, protocol_.kArrayLen - len);
                    if (len2 > 0) {
                        len += len2;
                    }
                } catch (SocketTimeoutException excepTO) {
                }
                protocol_.socket_.setSoTimeout(30000); //Set previous timeout
            }
        } catch (SocketTimeoutException e2) {
            e2.printStackTrace();
            protocol_.arrayProvider_.returnArray(arrayElem_);
            throw new ProtocolException("Socket timeout", ProtocolException.ErrorCode.SOCKET_ERROR);
        } catch (SocketException e1) {
            e1.printStackTrace();
            protocol_.arrayProvider_.returnArray(arrayElem_);
            throw new ProtocolException("Error setting socket timeout", ProtocolException.ErrorCode.SOCKET_ERROR);
        } catch (IOException e) {
            e.printStackTrace();
            protocol_.arrayProvider_.returnArray(arrayElem_);
            throw new ProtocolException("Error reading", ProtocolException.ErrorCode.READER_ERROR);
        } 
        
        if (len == -1) {
            protocol_.arrayProvider_.returnArray(arrayElem_);
            throw new ProtocolException("Closed", ProtocolException.ErrorCode.CLOSED);
        } else {
            int i;
            int totalNumbers = 0;
            int nextNumber = -1;
            int firstPosOfNewLine = protocol_.kIntLen + 1;
            int lastPosOfNewLine = protocol_.kIntLen + newLineLen_;
            boolean terminate = false;
            for(i = 0; i < len && !terminate; i++) {
                int valToCheck = (i + 1) % lastPosOfNewLine;
                if (valToCheck != 0 && valToCheck < firstPosOfNewLine) {
                    if (arrayElem_.array_[i] < '0' || arrayElem_.array_[i] > '9') {
                        if ((i + 1) % (protocol_.kIntLen + newLineLen_) == 1 && arrayElem_.array_[i] == 't') {
                            terminate = true;
                        } else {
                            if (protocol_.dataConsumer_ != null && totalNumbers > 0) {
                                protocol_.dataConsumer_.processData(arrayElem_, 0, 
                                        totalNumbers, (protocol_.kIntLen + newLineLen_));
                            } else {
                                protocol_.arrayProvider_.returnArray(arrayElem_);
                            }
                            throw new ProtocolException("Erroneous data", ProtocolException.ErrorCode.READER_ERROR);
                        }
                    } // else OK
                } else {
                    char nexNewLineCharExpected = newLineChars_[nextNewLineIdx_];
                    nextNewLineIdx_ = (nextNewLineIdx_ + 1) % newLineLen_; 
                    if (arrayElem_.array_[i] != nexNewLineCharExpected) {
                        if (protocol_.dataConsumer_ != null && totalNumbers > 0) {
                            protocol_.dataConsumer_.processData(arrayElem_, 0, 
                                    totalNumbers, (protocol_.kIntLen + newLineLen_));
                        } else {
                            protocol_.arrayProvider_.returnArray(arrayElem_);
                        }
                        throw new ProtocolException("Erroneous data", ProtocolException.ErrorCode.READER_ERROR);
                    } else {
                        if (nextNewLineIdx_ == 0) {
                            nextNumber = i + 1; //This is OK, next number begins at next position
                            totalNumbers++;
                        }
                    }
                }
            }
            if (!terminate) {
                ArrayProvider.ArrayElement nextArrayElem = protocol_.arrayProvider_.getArray();
                if (nextNumber == -1) {
                    nextNumber = 0;
                }
                nextOffset_ = 0;
                if (nextNumber < len) {
                    int copyIdx;
                    for(copyIdx = nextNumber; copyIdx < len; copyIdx++) {
                        nextArrayElem.array_[copyIdx - nextNumber] = arrayElem_.array_[copyIdx];
                        nextOffset_++;
                    }
                }
                if (protocol_.dataConsumer_ != null && totalNumbers > 0) {
                    protocol_.dataConsumer_.processData(arrayElem_, 0, 
                            totalNumbers, (protocol_.kIntLen + newLineLen_));
                } else {
                    protocol_.arrayProvider_.returnArray(arrayElem_);
                }
                arrayElem_ = nextArrayElem;
            } else {
                nextState = new TerminateState(protocol_, arrayElem_.array_, i - 1, len - 1);
                if (protocol_.dataConsumer_ != null && totalNumbers > 0) {
                    protocol_.dataConsumer_.processData(arrayElem_, 0, 
                            totalNumbers, (protocol_.kIntLen + newLineLen_));
                } else {
                    protocol_.arrayProvider_.returnArray(arrayElem_);
                }
            }
        }
        return nextState;
    }
}

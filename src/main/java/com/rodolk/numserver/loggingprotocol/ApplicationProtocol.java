package com.rodolk.numserver.loggingprotocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * ApplicationProtocol manages the defined application protocol for a single connection.
 * Each connection must have one instance of ApplciationProtocol.
 * The main method is execute() and it will run through different until it is indicated
 * to terminate by another object. It can also terminate through a ProtocolException:
 * <ul>
 * <li>For connection closed (ProtocolException.ErrorCode.CLOSED)
 * <li>For 'terminate' indicated by client (ProtocolException.ErrorCode.END)
 * <li>For error in input data or other socket error
 * </ul>
 * <p>
 * In order for the read data to be processed a DataConsumer must be subscribed with
 * dataConsumerSubscribe
 * <p>
 * This class implements a state pattern where every state inherits from State.
 * 
 * @author rodolk
 *
 */
public class ApplicationProtocol {
    public static int kArrayLen = 1460 * 2; //2 (typical MTUs - 40) for 2 full packets
    public static int kMinThreshold = kArrayLen / 10; 
    public static int kIntLen = 9;
    public static int kIntTotLen = 10;
    private static char[] KNewLine = {'\r','\n'}; 
    private static int KNewLineLen = 2; 
    public static String kTerminateString = "terminate";
    public static boolean systemNewLine = true;
    Socket socket_;
    State state_;
    BufferedReader inBuffReader_;
    ArrayProvider arrayProvider_;
    DataConsumer dataConsumer_ = null;
    boolean terminate_ = false;
    
    public static char[] getNewLine() {
        if (systemNewLine) {
            return System.getProperty("line.separator").toCharArray();
        } else {
            return KNewLine;
        }
    }
    public static int getNewLineLen() {
        if (systemNewLine) {
            return System.getProperty("line.separator").length();
        } else {
            return KNewLineLen;
        }
    }
    
    public ApplicationProtocol(Socket socket, ArrayProvider prov) throws ProtocolException {
        state_ = new IdleState(this);
        socket_ = socket;
        arrayProvider_ = prov;
    }
    
    public void dataConsumerSubscribe(DataConsumer consumer) {
        dataConsumer_ = consumer;
    }
    
    public void setTerminate() {
        terminate_ = true;
    }
    
    public void execute() throws ProtocolException{
        try {
            socket_.setSoTimeout(30000);
        } catch (SocketException e1) {
            e1.printStackTrace();
            try {
                inBuffReader_.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            while(!terminate_) {
                state_ = state_.process();
            }
        } catch(ProtocolException exception) {
            if (exception.getError() == ProtocolException.ErrorCode.CLOSED) {
                System.out.println("Thread " + Thread.currentThread().getId() + " finished normally because client closed connection");
            } else if (exception.getError() == ProtocolException.ErrorCode.END) {
                System.out.println("Thread " + Thread.currentThread().getId() + " finished normally because client indicated terminate");
                System.out.println("Terminate whole process");
                throw exception;
            } else {
                System.out.println("Thread " + Thread.currentThread().getId() + " finished with error: " +exception.getError() + " Message: " + exception.getMessage());
            }
        } finally {
            try {
                inBuffReader_.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            state_ = null;
            socket_ = null;
            arrayProvider_ = null;
        }
    }
}

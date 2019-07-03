package com.rodolk.numserver.logserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Connection listener is a thread that listens to the server socket waiting for new connections
 * 
 * @author rodolk
 *
 */
public class ConnectionsListener extends Thread {
    ServerSocket sSocket_;
    short port_;
    List<NewConnectionSubscriber> subscribersList_ = new ArrayList<NewConnectionSubscriber>();
    boolean end_ = false;
    
    public interface NewConnectionSubscriber {
        public void processConnection(Socket socket);
    }
    
    public ConnectionsListener(short port) {
        port_ = port;
    }
    
    public void subscribe(NewConnectionSubscriber sub) {
        subscribersList_.add(sub);
    }
    
    public void setEnd() {
        end_ = true;
    }
    
    public void run() {
        try {
            sSocket_ = new ServerSocket(port_);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Error initiating server. Finishing ConnectionListener thread");
            return;
        }
        
        while(!end_) {
            try {
                Socket newSocket = sSocket_.accept();
                if (!end_) {
                    for(NewConnectionSubscriber sub : subscribersList_) {
                        sub.processConnection(newSocket);
                    }
                }
            } catch (IOException e) {
                //Just continue listening
                e.printStackTrace();
            } 
        }
        
        try {
            sSocket_.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

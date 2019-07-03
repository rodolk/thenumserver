package com.rodolk.numserver.logserver;


public class Main {

    public static void main(String[] args) {

        System.out.println("Starting up server ....");
    ServerManager sm = new ServerManager();
    sm.startAll();
    sm.execute();
    System.out.println("Server execution finished");
    }
}

package com.rodolk.numserver.logserver;

public abstract class Observer {
    public abstract void processEvent(Subject.Event evt);
    protected void subscribe(Subject subj) {
        subj.subscribe(this);
    }
}

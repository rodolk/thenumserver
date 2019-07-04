package com.rodolk.numserver.logserver;

/**
 * Generic observer for Observer pattern.
 * 
 * @author rodolk
 *
 */
public abstract class Observer {
    public abstract void processEvent(Subject.Event evt);
    protected void subscribe(Subject subj) {
        subj.subscribe(this);
    }
}

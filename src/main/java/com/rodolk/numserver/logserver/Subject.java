package com.rodolk.numserver.logserver;

import java.util.ArrayList;
import java.util.List;

public abstract class Subject {
    List<Observer> observersList_ = new ArrayList<Observer>();
    
    public class Event {
        private int type_ = 0;
        
        public int getType() {
            return type_;
        }
        
        protected void setType(int type) {
            type_ = type;
        }
    }
    
    public void subscribe(Observer obs) {
        observersList_.add(obs);
    }
    
    void notify(Event evt) {
        for(Observer obs : observersList_) {
            obs.processEvent(evt);
        }
    }

}

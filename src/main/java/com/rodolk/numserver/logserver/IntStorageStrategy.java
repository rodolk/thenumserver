package com.rodolk.numserver.logserver;

public abstract class IntStorageStrategy {
    synchronized public boolean contains(char[] array, int offset, int len) {
        return hasValue(array, offset, len);
    }
    
    protected abstract boolean hasValue(char[] array, int offset, int len);
    public abstract int getSize();
}

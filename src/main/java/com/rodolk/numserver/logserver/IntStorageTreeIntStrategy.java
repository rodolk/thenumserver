package com.rodolk.numserver.logserver;

import java.util.TreeMap;

/**
 * This class is part of a Strategy pattern. It uses a a red-black tree, a kind of balanced binary tree, to store elements.
 * It requires a number passed in an array without new line characters.
 * 
 * @author rodolk
 *
 */
public class IntStorageTreeIntStrategy extends IntStorageStrategy{
    TreeMap<Integer, Boolean> treeMap_ = new TreeMap<Integer, Boolean>();
    
    /**
     * Returns the tree size
     * 
     */
    synchronized public int getSize() {
        return treeMap_.size();
    }
    
    /**
     * Determines if a value exists in the tree.
     * 
     * @return  true if value exists.
     *          false if value does not exist. In this case the value is added to the tree.
     *          
     */
    @Override
    protected boolean hasValue(char[] array, int offset, int len) {
        int value = 0;
        int tmp;
        int pos = 0;
        for(pos = 0;pos < len; pos++) {
            tmp = array[offset + pos] - '0';
            value += tmp * Math.pow(10, (len - 1 - pos));
        }
        if (!treeMap_.containsKey(value)) {
            treeMap_.put(value, true);
            return false;
        } else {
            return true;
        }
    }
}

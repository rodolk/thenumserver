package com.rodolk.numserver.logserver;

import java.util.TreeMap;

public class IntStorageTreeIntStrategy extends IntStorageStrategy{
	TreeMap<Integer, Boolean> treeMap_ = new TreeMap<Integer, Boolean>();
	
	public int getSize() {
		return treeMap_.size();
	}
	
	@Override
	protected boolean hasValue(char[] array, int offset, int len) {
		int value = 0;
		int tmp;
		int pos = 0;
		for(pos = 0;pos < len - 1; pos++) {
			tmp = array[offset + pos] - '0';
			value += tmp * Math.pow(10, (len - 2 - pos));
		}
		if (!treeMap_.containsKey(value)) {
			treeMap_.put(value, true);
			return false;
		} else {
			return true;
		}
	}
}

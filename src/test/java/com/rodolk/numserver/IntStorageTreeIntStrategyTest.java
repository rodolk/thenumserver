package com.rodolk.numserver;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import com.rodolk.numserver.logserver.IntStorageTreeIntStrategy;

public class IntStorageTreeIntStrategyTest {
    public static final int kNumberLen_  = 10;
    public static final int kNewLineLen_ = 1;
    
    @Test
    public void testHasValue() {
        //fail("Not yet implemented");
    }

    @Test
    public void testContains() {
        IntStorageTreeIntStrategy intStorage = new IntStorageTreeIntStrategy();
        final int maxSize = 20000000;
        char[] charArray = new char[maxSize];
        int value;
        //Random r = new Random();
        int i;
        int pos = maxSize - 1;
        value = 9999999;
        for(i = 0; i < (maxSize / 10); i++) {
            char[] ca = String.valueOf(value).toCharArray();
            int value2 = value % 10;
            int value3 = Math.floorDiv(value, 10);
            charArray[pos] = '\n';
            for(int j = pos - 1; j > (pos - 10); j--) {
                charArray[j] = (char) (value2 + '0');
                value2 = value3 % 10;
                value3 = Math.floorDiv(value3, 10);
            }
            pos -= 10;
            value++;
            boolean res = intStorage.contains(charArray, pos + 1, kNumberLen_ - kNewLineLen_);
/*            if (res == false) {
                for(int p = pos + 1; p < pos + 11; p++) {
                    System.out.print(charArray[p]);
                }
            } */
            assertEquals(res, false);
        }
        pos = maxSize - 1;
        value = 9999999;
        for(i = 0; i < (maxSize / 10); i++) {
            char[] ca = String.valueOf(value).toCharArray();
            int value2 = value % 10;
            int value3 = Math.floorDiv(value, 10);
            charArray[pos] = '\n';
            for(int j = pos - 1; j > (pos - 10); j--) {
                charArray[j] = (char) (value2 + '0');
                value2 = value3 % 10;
                value3 = Math.floorDiv(value3, 10);
            }
            pos -= kNumberLen_;
            value++;
            boolean res = intStorage.contains(charArray, pos + 1, kNumberLen_ - kNewLineLen_);
            assertEquals(res, true);
        }
    }

}

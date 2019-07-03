package com.rodolk.numserver.logserver;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Statistics is a singleton that provides statistics from the system. Specifically, it
 * provides number of unique numbers and duplicate numbers received since the last time a snapshot was taken.
 * For obtaining latest figures first a client needs to call setSnapshot. Then it will get every value.
 * Statistics are gathered in StatisticsCounter's that are passed to different threads.
 * Then when a snapshot is requested, Statistics will go through all StatisticsCounter's, get their information and
 * accumulate in its own variables. 
 * Something to note is that these classes don't use lockers/mutexes and this is good for performance.
 * We can work in this way because every thread will update its own counters, so there is no critical section.
 * Additionally Statistics needs to access the data from every StatisticsCounter's. But this is only a read that we can do 
 * without mutual exclusion. Race conditions between read and write do not affect the result: a unique number that was processed at 
 * the period limit, can appear in the current period reporting or in the next period reporting but not twice. We don't care in which period
 * a number appearing at the limit will be shown. So by relaxing this requirement we avoid locking a thread once per processed number and
 * we gain performance.
 * 
 * @author rodolk
 *
 */
public class Statistics {
    private static Statistics instance_ = null;
    private static Lock lock_;
    private static int indexes_;
    private static boolean initialized_ = false;
    
    private StatisticsCounter[] statsCounterArr;
    private long   totalUniqueCounter_;
    private long   periodUniqueCounter_;
    private long   periodDuplicateCounter_;
    
    /**
     * StatisticsCounter are objects passed to the thread that need to update their counters.
     * Statistics keeps track of every StatisticsCounter, and takes snapshots when necessary by calling setSnapshot,
     * which is private.
     * 
     * @author rodolk
     *
     */
    public class StatisticsCounter {
        private long uniqueCounter_;
        private long duplicateCounter_;
        private long lastUniqueCounter_;
        private long lastDuplicateCounter_;
        
        private StatisticsCounter() {
            uniqueCounter_ = 0;
            duplicateCounter_ = 0;
            lastUniqueCounter_ = 0;
            lastDuplicateCounter_ = 0;
        }
        
        private void setSnapshot() {
            lastUniqueCounter_ = uniqueCounter_;
            lastDuplicateCounter_ = duplicateCounter_; 
        }

        public void incUnique() {
            uniqueCounter_++;
        }
        
        public void incDuplicate() {
            duplicateCounter_++;
        }
        
    }

    public static void initialize(int indexes) {
        lock_ = new ReentrantLock();
        indexes_ = indexes;
        initialized_ = true;
    }
    
    public static Statistics getInstance() {
        if (!initialized_) return null;
        
        if (instance_ == null) {
            lock_.lock();
            try {
                if (instance_ == null) {
                    instance_ = new Statistics(indexes_);
                }
            } finally {
                lock_.unlock();
            }
        }
        
        return instance_;
    }

    private Statistics(int indexes) {
        indexes_ = indexes;
        statsCounterArr = new StatisticsCounter[indexes];
        for(int i = 0; i < indexes; i ++) {
            statsCounterArr[i] = new StatisticsCounter();
        }
        
        totalUniqueCounter_ = 0;
        periodUniqueCounter_ = 0;
        periodDuplicateCounter_ = 0;
    }
    
    public StatisticsCounter[] getStatisticsCounterArray() {
        return statsCounterArr;
    }
    
    public void setSnapshot() {
        periodUniqueCounter_ = 0;
        periodDuplicateCounter_ = 0;
        for(int i = 0; i < indexes_; i++ ) {
            long lastUniqueCounter = statsCounterArr[i].lastUniqueCounter_;
            long lastDuplicateCounter = statsCounterArr[i].lastDuplicateCounter_;
            statsCounterArr[i].setSnapshot();
            periodUniqueCounter_    += statsCounterArr[i].lastUniqueCounter_ - lastUniqueCounter;
            periodDuplicateCounter_ += statsCounterArr[i].lastDuplicateCounter_ - lastDuplicateCounter;
        }
        totalUniqueCounter_ += periodUniqueCounter_;
    }
    
    public long getTotalUniqueCounter() {
        return totalUniqueCounter_;
    }
    
    public long getPeriodUniqueCounter() {
        return periodUniqueCounter_;
    }
    
    public long getPeriodDuplicateCounter() {
        return periodDuplicateCounter_;
    }
}

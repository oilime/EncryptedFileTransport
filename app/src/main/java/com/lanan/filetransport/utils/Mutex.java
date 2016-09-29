package com.lanan.filetransport.utils;

public class Mutex {
    private boolean syncLock;

    public Mutex() {
        syncLock = false;
    }

    public synchronized void lock() {
        while(syncLock) {
            try {
                wait();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        syncLock = true;
    }

    public synchronized void unlock() {
        syncLock = false;
        notifyAll();
    }
}

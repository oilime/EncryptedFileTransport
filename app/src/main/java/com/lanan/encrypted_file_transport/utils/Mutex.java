package com.lanan.encrypted_file_transport.utils;

/**
 * Created by lanan on 16-5-10.
 */

public class Mutex {
    private boolean syncLock;

    public Mutex() {
        syncLock = false;
    }

    public synchronized void lock() {
        while(syncLock == true) {
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

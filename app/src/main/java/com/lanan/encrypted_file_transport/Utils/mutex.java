package com.lanan.encrypted_file_transport.Utils;

public class mutex {
    private boolean syncLock;

    public mutex() {
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

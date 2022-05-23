package com.familygem.action;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorSingleton {
    private static ExecutorSingleton INSTANCE = null;

    // other instance variables can be here
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ExecutorSingleton() {}

    public static ExecutorSingleton getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ExecutorSingleton();
        }
        return(INSTANCE);
    }

    public  ExecutorService getExecutor() {
        return executor;
    }

}

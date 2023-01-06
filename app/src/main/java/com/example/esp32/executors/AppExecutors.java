package com.example.esp32.executors;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AppExecutors {
    public static AppExecutors instance;

    private final ScheduledExecutorService newWorkIO =
            Executors.newSingleThreadScheduledExecutor();

    // Singleton
    public static AppExecutors getInstance() {
        if (instance == null) {
            instance = new AppExecutors();
        }
        return instance;
    }

    // Getter
    public ScheduledExecutorService netWorkIO(){
        return newWorkIO;
    }
}

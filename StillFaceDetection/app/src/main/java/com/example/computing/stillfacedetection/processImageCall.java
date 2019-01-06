package com.example.computing.stillfacedetection;

/**
 * Created by Computing on 12/08/2016.
 */

import java.lang.InterruptedException;
import java.lang.String;
import java.lang.Object;
import java.util.concurrent.CountDownLatch;

public class processImageCall {
    private final String methodName;
    private final Object[] args;
    private final CountDownLatch resultReady;
    private Object result;

    public processImageCall(String methodName, Object[] args) {
        this.methodName = methodName;
        this.args = args;
        this.resultReady = new CountDownLatch(1);
    }
    public processImageCall() {
        this.methodName = "batch thread";
        this.args = new Object[]{};
        this.resultReady = new CountDownLatch(1);
    }

    public void setResult(Object result){
        this.result = result;
        resultReady.countDown();
    }

    public Object getResult() throws InterruptedException{
        resultReady.await();
        return result;
    }

}

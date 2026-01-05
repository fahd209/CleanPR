package com.fahd.cleanPR.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * this abstract class is base
 * for all the event handler
 * classes
 * */
public abstract class BaseEventHandler {

    private final Logger logger =  LoggerFactory.getLogger(getClass());

    public abstract void triggerEvent(Map<String, Object> webHookPayload, String action);

    public void logInfo(String message) {
        logger.info(message);
    }

    public void logError(String message) {
        logger.info(message);
    }
}

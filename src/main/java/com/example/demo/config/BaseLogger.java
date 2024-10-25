package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseLogger {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
}

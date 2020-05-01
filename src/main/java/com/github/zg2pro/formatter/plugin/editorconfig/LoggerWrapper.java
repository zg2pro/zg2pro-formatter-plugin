package com.github.zg2pro.formatter.plugin.editorconfig;

import org.ec4j.lint.api.Logger;


public class LoggerWrapper extends Logger.AbstractLogger {
    
    private final org.slf4j.Logger delegate;
    
    private static LogLevelSupplier toEc4jLogLevelSupplier(final org.slf4j.Logger log) {
        return () -> {
            if (log.isTraceEnabled()) {
                return LogLevel.TRACE;
            } else if (log.isDebugEnabled()) {
                return LogLevel.DEBUG;
            } else if (log.isInfoEnabled()) {
                return LogLevel.INFO;
            } else if (log.isWarnEnabled()) {
                return LogLevel.WARN;
            } else if (log.isErrorEnabled()) {
                return LogLevel.ERROR;
            } else {
                return LogLevel.NONE;
            }
        };
    }

    public LoggerWrapper(org.slf4j.Logger delegate) {
        super(toEc4jLogLevelSupplier(delegate));
        this.delegate = delegate;
    }

    @Override
    public void log(LogLevel level, String string, Object... args) {
        switch (level) {
            case TRACE:
                delegate.trace(string, args);
                break;
            case DEBUG:
                delegate.debug(string, args);
                break;
            case INFO:
                delegate.info(string, args);
                break;
            case WARN:
                delegate.warn(string, args);
                break;
            default:
                delegate.error(string, args);
        }
    }

}
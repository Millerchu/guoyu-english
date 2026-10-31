package com.guoyu.english.auth;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.slf4j.LoggerFactory;

/**
 * 收集测试期间的日志文本，用来确认没有打出 PIN、引导码和令牌。
 */
public final class LogCapture implements AutoCloseable {

    private final Logger root;

    private final ListAppender<ILoggingEvent> appender;

    public LogCapture() {
        root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
    }

    public String text() {
        StringBuilder builder = new StringBuilder();
        for (ILoggingEvent event : appender.list) {
            builder.append(event.getFormattedMessage()).append('\n');
            if (event.getThrowableProxy() != null) {
                builder.append(String.valueOf(event.getThrowableProxy().getMessage())).append('\n');
            }
        }
        return builder.toString();
    }

    @Override
    public void close() {
        root.detachAppender(appender);
        appender.stop();
    }
}

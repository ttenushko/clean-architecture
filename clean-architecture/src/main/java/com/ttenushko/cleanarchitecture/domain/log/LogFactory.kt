package com.ttenushko.cleanarchitecture.domain.log

public interface LogFactory {
    public fun get(tag: String): Log
    public fun addWatcher(watcher: Watcher)
    public fun removeWatcher(watcher: Watcher)

    public interface Watcher {
        public fun logMessage(
            logLevel: LogLevel,
            tag: String,
            message: String,
            throwable: Throwable?
        )

        public enum class LogLevel {
            VERBOSE,
            DEBUG,
            INFO,
            WARNING,
            ERROR
        }
    }
}
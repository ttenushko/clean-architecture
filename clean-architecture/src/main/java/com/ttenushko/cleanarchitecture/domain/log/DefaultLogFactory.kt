package com.ttenushko.cleanarchitecture.domain.log

import com.ttenushko.cleanarchitecture.domain.log.LogFactory.Watcher
import com.ttenushko.cleanarchitecture.domain.log.LogFactory.Watcher.LogLevel
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet

public class DefaultLogFactory : LogFactory {

    private val watchers = CopyOnWriteArraySet<Watcher>()
    private val logs = HashMap<String, WeakReference<Log>>()
    private val sync = Any()

    override fun get(tag: String): Log =
        synchronized(this) {
            logs[tag]?.get() ?: LogImpl(tag, ::handleLogMessage).also {
                logs[tag] = WeakReference<Log>(it)
            }
        }

    override fun addWatcher(watcher: Watcher) {
        watchers.add(watcher)
    }

    override fun removeWatcher(watcher: Watcher) {
        watchers.remove(watcher)
    }

    private fun handleLogMessage(
        logLevel: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?
    ) = synchronized(sync) {
        watchers.forEach { it.logMessage(logLevel, tag, message, throwable) }
    }

    private class LogImpl(
        private val tag: String,
        private val handleLogMessage: (
            logLevel: LogLevel,
            tag: String,
            message: String,
            throwable: Throwable?
        ) -> Unit
    ) : Log {

        override fun v(lazyMsg: () -> Any?) {
            handleLogMessage(
                LogLevel.VERBOSE,
                tag,
                lazyMsg()?.asString() ?: "",
                null
            )
        }

        override fun v(throwable: Throwable, lazyMsg: () -> Any?) {
            handleLogMessage(
                LogLevel.VERBOSE,
                tag,
                lazyMsg()?.asString() ?: "",
                throwable
            )
        }

        override fun d(lazyMsg: () -> Any?) {
            handleLogMessage(
                LogLevel.DEBUG,
                tag,
                lazyMsg()?.asString() ?: "",
                null
            )
        }

        override fun d(throwable: Throwable, lazyMsg: () -> Any?) {
            handleLogMessage(
                LogLevel.DEBUG,
                tag,
                lazyMsg()?.asString() ?: "",
                throwable
            )
        }

        override fun i(lazyMsg: () -> Any?) {
            handleLogMessage(
                LogLevel.INFO,
                tag,
                lazyMsg()?.asString() ?: "",
                null
            )
        }

        override fun i(throwable: Throwable, lazyMsg: () -> Any?) {
            handleLogMessage(
                LogLevel.INFO,
                tag,
                lazyMsg()?.asString() ?: "",
                throwable
            )
        }

        override fun w(lazyMsg: () -> Any?) {
            handleLogMessage(
                LogLevel.WARNING,
                tag,
                lazyMsg()?.asString() ?: "",
                null
            )
        }

        override fun w(throwable: Throwable, lazyMsg: () -> Any?) {
            handleLogMessage(
                LogLevel.WARNING,
                tag,
                lazyMsg()?.asString() ?: "",
                throwable
            )
        }

        override fun e(lazyMsg: () -> Any?) {
            handleLogMessage(
                LogLevel.ERROR,
                tag,
                lazyMsg()?.asString() ?: "",
                null
            )
        }

        override fun e(throwable: Throwable, lazyMsg: () -> Any?) {
            handleLogMessage(
                LogLevel.ERROR,
                tag,
                lazyMsg()?.asString() ?: "",
                throwable
            )
        }

        private fun Any.asString(): String =
            if (this is String) this
            else this.toString()
    }
}
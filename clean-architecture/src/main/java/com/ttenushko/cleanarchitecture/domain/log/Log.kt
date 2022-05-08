package com.ttenushko.cleanarchitecture.domain.log

public interface Log {
    public fun v(lazyMsg: () -> Any?)
    public fun v(throwable: Throwable, lazyMsg: () -> Any?)
    public fun d(lazyMsg: () -> Any?)
    public fun d(throwable: Throwable, lazyMsg: () -> Any?)
    public fun i(lazyMsg: () -> Any?)
    public fun i(throwable: Throwable, lazyMsg: () -> Any?)
    public fun w(lazyMsg: () -> Any?)
    public fun w(throwable: Throwable, lazyMsg: () -> Any?)
    public fun e(lazyMsg: () -> Any?)
    public fun e(throwable: Throwable, lazyMsg: () -> Any?)
}
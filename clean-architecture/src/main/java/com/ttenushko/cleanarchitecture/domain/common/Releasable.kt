package com.ttenushko.cleanarchitecture.domain.common

interface Releasable {
    @Throws(Error::class)
    fun release()
}
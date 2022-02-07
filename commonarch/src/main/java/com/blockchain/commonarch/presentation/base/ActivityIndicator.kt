package com.blockchain.commonarch.presentation.base

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.concurrent.locks.ReentrantLock

class ActivityIndicator {

    private val lock = ReentrantLock()
    private val variable = BehaviorSubject.createDefault(0)

    val loading: Observable<Boolean> = variable.map { it > 0 }.distinctUntilChanged()
        .share()
        .replay(1)
        .refCount()

    fun <T : Any> Observable<T>.trackProgress(): Observable<T> {
        return this.doOnSubscribe {
            increment()
        }.doFinally {
            decrement()
        }
    }

    fun <T : Any> Single<T>.trackProgress(): Single<T> {
        return this.doOnSubscribe {
            increment()
        }.doFinally {
            decrement()
        }
    }

    fun <T> Maybe<T>.trackProgress(): Maybe<T> {
        return this.doOnSubscribe {
            increment()
        }.doFinally {
            decrement()
        }
    }

    fun Completable.trackProgress(): Completable {
        return this.doOnSubscribe {
            increment()
        }.doFinally {
            decrement()
        }
    }

    private fun increment() {
        lock.lock()
        variable.onNext(variable.value?.inc() ?: 1)
        lock.unlock()
    }

    private fun decrement() {
        lock.lock()
        variable.onNext(variable.value?.dec() ?: 0)
        lock.unlock()
    }
}

fun <T : Any> Observable<T>.trackProgress(activityIndicator: ActivityIndicator?): Observable<T> =
    activityIndicator?.let {
        with(it) {
            this@trackProgress.trackProgress()
        }
    } ?: this

fun <T : Any> Maybe<T>.trackProgress(activityIndicator: ActivityIndicator?): Maybe<T> =
    activityIndicator?.let {
        with(it) {
            this@trackProgress.trackProgress()
        }
    } ?: this

fun <T : Any> Single<T>.trackProgress(activityIndicator: ActivityIndicator?): Single<T> =
    activityIndicator?.let {
        with(it) {
            trackProgress()
        }
    } ?: this

fun Completable.trackProgress(activityIndicator: ActivityIndicator?): Completable =
    activityIndicator?.let {
        with(it) {
            this@trackProgress.trackProgress()
        }
    } ?: this

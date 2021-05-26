/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Read subscriber wrapper which remembers current state.
 * @param <T> Subscriber target type
 * @since 0.1
 */
final class ReadSubscriberState<T> implements Subscriber<T> {

    /**
     * Origin subscriber.
     */
    private final Subscriber<T> origin;

    /**
     * Completion state.
     */
    private final AtomicBoolean completed;

    /**
     * Decorates subscriber.
     * @param origin Subscriber to decorate
     */
    ReadSubscriberState(final Subscriber<T> origin) {
        this.origin = origin;
        this.completed = new AtomicBoolean();
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        this.origin.onSubscribe(subscription);
    }

    @Override
    public void onNext(final T next) {
        this.origin.onNext(next);
    }

    @Override
    public void onError(final Throwable err) {
        if (this.completed.compareAndSet(false, true)) {
            this.origin.onError(err);
        }
    }

    @Override
    public void onComplete() {
        if (this.completed.compareAndSet(false, true)) {
            this.origin.onComplete();
        }
    }

    /**
     * Cancel subscription.
     */
    public void cancel() {
        this.completed.set(true);
    }

    /**
     * Check if done.
     * @return True if done by any reason
     */
    public boolean done() {
        return this.completed.get();
    }
}

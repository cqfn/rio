/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

import java.util.concurrent.ExecutorService;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Async subscriber delegates all events to origin subscriber using
 * executor service.
 * @param <T> Subscriber target type
 * @since 0.2
 */
final class AsyncSubscriber<T> implements Subscriber<T> {

    /**
     * Origin subscriber.
     */
    private final Subscriber<T> origin;

    /**
     * Executor service.
     */
    private final ExecutorService execs;

    /**
     * Decorates subscriber.
     * @param origin Subscriber to decorate
     * @param exec Executor service
     */
    AsyncSubscriber(final Subscriber<T> origin, final ExecutorService exec) {
        this.origin = origin;
        this.execs = exec;
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        this.exec(() -> this.origin.onSubscribe(subscription), false);
    }

    @Override
    public void onNext(final T next) {
        this.exec(() -> this.origin.onNext(next), false);
    }

    @Override
    public void onError(final Throwable err) {
        this.exec(() -> this.origin.onError(err), true);
    }

    @Override
    public void onComplete() {
        this.exec(() -> this.origin.onComplete(), true);
    }

    /**
     * Execute with a service.
     * @param task To execute
     * @param close To shutdown a service after exec
     */
    private void exec(final Runnable task, final boolean close) {
        if (this.execs == null) {
            task.run();
        } else {
            this.execs.submit(
                () -> {
                    task.run();
                    if (close) {
                        this.execs.shutdown();
                    }
                }
            );
        }
    }
}

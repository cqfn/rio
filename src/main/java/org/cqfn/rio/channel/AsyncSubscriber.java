/*
 * MIT License
 *
 * Copyright (c) 2020 cqfn.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights * to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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

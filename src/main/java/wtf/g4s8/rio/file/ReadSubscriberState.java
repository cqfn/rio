/*
 * MIT License
 *
 * Copyright (c) 2020 g4s8
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
package wtf.g4s8.rio.file;

import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Read subscriber wrapper which remembers current state.
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
    public ReadSubscriberState(final Subscriber<T> origin) {
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

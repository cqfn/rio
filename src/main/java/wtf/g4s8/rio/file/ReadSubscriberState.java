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

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Read subscriber wrapper which remembers current state.
 * @since 0.1
 */
final class ReadSubscriberState<T> implements Subscriber<T> {

    /**
     * Origin subscriber weak reference.
     * <p>
     * According to RS-TCK specification, publisher can't hold subscriber references after
     * cancellation, so we're clearing reference manually on cancel signals.
     * </p>
     */
    private final WeakReference<Subscriber<T>> origin;

    /**
     * Completion state.
     */
    private final AtomicBoolean completed;

    /**
     * Decorates subscriber.
     * @param origin Subscriber to decorate
     */
    public ReadSubscriberState(final Subscriber<T> origin) {
        this.origin = new WeakReference<>(origin);
        this.completed = new AtomicBoolean();
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        final Subscriber<T> sub = this.origin.get();
        if (sub == null) {
            return;
        }
        sub.onSubscribe(subscription);
    }

    @Override
    public void onNext(final T next) {
        final Subscriber<T> sub = this.origin.get();
        if (sub == null) {
            return;
        }
        sub.onNext(next);
    }

    @Override
    public void onError(final Throwable err) {
        final Subscriber<T> sub = this.origin.get();
        if (sub == null) {
            return;
        }
        if (this.completed.compareAndSet(false, true)) {
            sub.onError(err);
            this.origin.clear();
        }
    }

    @Override
    public void onComplete() {
        final Subscriber<T> sub = this.origin.get();
        if (sub == null) {
            return;
        }
        if (this.completed.compareAndSet(false, true)) {
            sub.onComplete();
            this.origin.clear();
        }
    }

    /**
     * Cancel subscription.
     */
    public void cancel() {
        this.completed.set(true);
        this.origin.clear();
    }

    /**
     * Check if done.
     * @return True if done by any reason
     */
    public boolean done() {
        return this.completed.get();
    }
}

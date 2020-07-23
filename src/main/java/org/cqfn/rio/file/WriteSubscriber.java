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
package org.cqfn.rio.file;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Write to channel subscriber.
 * @since 0.1
 */
final class WriteSubscriber extends CompletableFuture<Void> implements Subscriber<ByteBuffer> {

    /**
     * Channel to write.
     */
    private final FileChannel chan;

    /**
     * Subscription reference.
     */
    private final AtomicReference<Subscription> sub;

    /**
     * Executor service.
     */
    private final ExecutorService exec;

    /**
     * Write greed level.
     */
    private final WriteGreed greed;

    /**
     * Tasks queue for write requests.
     */
    private WriteTaskQueue queue;

    /**
     * New write subscriber.
     * @param chan File channel
     * @param greed Consumer greed level
     * @param exec Executor service to process requests
     */
    WriteSubscriber(final FileChannel chan, final WriteGreed greed, final ExecutorService exec) {
        this.chan = chan;
        this.sub = new AtomicReference<>();
        this.exec = exec;
        this.greed = greed;
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        if (!this.sub.compareAndSet(null, Objects.requireNonNull(subscription))) {
            subscription.cancel();
            return;
        }
        if (this.isCancelled()) {
            subscription.cancel();
        } else {
            this.queue = new WriteTaskQueue(
                this, this.chan, this.sub, this.greed, this.exec
            );
            this.queue.accept(new WriteRequest.Init(this));
        }
    }

    @Override
    public void onNext(final ByteBuffer buf) {
        this.queue.accept(new WriteRequest.Next(this, Objects.requireNonNull(buf)));
    }

    @Override
    public void onError(final Throwable err) {
        this.queue.accept(new WriteRequest.Error(this, Objects.requireNonNull(err)));
    }

    @Override
    public void onComplete() {
        this.queue.accept(new WriteRequest.Complete(this));
    }
}

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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.cqfn.rio.WriteGreed;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Write to channel subscriber.
 * @since 0.1
 */
final class WritableChannelSubscriber extends CompletableFuture<Void>
    implements Subscriber<ByteBuffer> {

    /**
     * Channel to write.
     */
    private final ChannelSource<? extends WritableByteChannel> src;

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
     * @param src Source of channel
     * @param greed Consumer greed level
     * @param exec Executor service to process requests
     */
    WritableChannelSubscriber(final ChannelSource<? extends WritableByteChannel> src,
        final WriteGreed greed, final ExecutorService exec) {
        this.src = src;
        this.sub = new AtomicReference<>();
        this.exec = exec;
        this.greed = greed;
    }

    /**
     * Accept publisher asynchronous and ask it to subscribe.
     * @param publisher Of data
     */
    public void acceptAsync(final Publisher<ByteBuffer> publisher) {
        this.exec.submit(() -> publisher.subscribe(this));
    }

    // @checkstyle ReturnCountCheck (25 lines)
    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public void onSubscribe(final Subscription subscription) {
        if (!this.sub.compareAndSet(null, Objects.requireNonNull(subscription))) {
            subscription.cancel();
            return;
        }
        if (this.isCancelled()) {
            subscription.cancel();
            return;
        }
        final WritableByteChannel chan;
        try {
            chan = this.src.channel();
        } catch (final IOException iex) {
            subscription.cancel();
            this.completeExceptionally(iex);
            return;
        }
        this.queue = new WriteTaskQueue(this, chan, this.sub, this.greed, this.exec);
        this.greed.init(subscription);
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

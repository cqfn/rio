/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

import org.cqfn.rio.WriteGreed;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Write to channel subscriber.
 *
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
     *
     * @param src   Source of channel
     * @param greed Consumer greed level
     * @param exec  Executor service to process requests
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
     *
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

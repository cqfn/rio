/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import org.cqfn.rio.Buffers;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * File read flow publisher.
 * @since 0.2
 * @checkstyle ParameterNumberCheck (500 lines)
 */
final class ReadableChannelPublisher implements Publisher<ByteBuffer> {

    /**
     * Dummy subscription which does nothing.
     */
    private static final Subscription DUMMY = new Subscription() {
        @Override
        public void request(final long count) {
            // nothing
        }

        @Override
        public void cancel() {
            // nothing
        }
    };

    /**
     * Channel source.
     */
    private final ChannelSource<? extends ReadableByteChannel> src;

    /**
     * Buffer allocation strategy.
     */
    private final Buffers buffers;

    /**
     * Executor service for IO operations.
     */
    private final ExecutorService exec;

    /**
     * Ctor.
     * @param src Channel
     * @param buffers Buffers allocation strategy
     * @param exec Executor service for IO operations
     */
    ReadableChannelPublisher(final ReadableByteChannel src,
        final Buffers buffers, final ExecutorService exec) {
        this(() -> src, buffers, exec);
    }

    /**
     * Ctor.
     * @param src Source of channel
     * @param buffers Buffers allocation strategy
     * @param exec Executor service for IO operations
     */
    ReadableChannelPublisher(final ChannelSource<? extends ReadableByteChannel> src,
        final Buffers buffers, final ExecutorService exec) {
        this.src = src;
        this.buffers = buffers;
        this.exec = exec;
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
        Objects.requireNonNull(subscriber, "Subscriber can't be null");
        final ReadableByteChannel chan;
        try {
            chan = this.src.channel();
        } catch (final IOException err) {
            subscriber.onSubscribe(ReadableChannelPublisher.DUMMY);
            subscriber.onError(err);
            return;
        }
        final ReadSubscriberState<? super ByteBuffer> wrap = new ReadSubscriberState<>(subscriber);
        wrap.onSubscribe(
            new ReadSubscription(
                wrap, this.buffers,
                new ReadTaskQueue(wrap, chan, this.exec)
            )
        );
    }
}

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
     * Executor service for subscribers.
     */
    private final ExecutorService subex;

    /**
     * Ctor.
     * @param src Channel
     * @param buffers Buffers allocation strategy
     * @param exec Executor service for IO operations
     * @param subex Executor service of subscriber
     */
    ReadableChannelPublisher(final ReadableByteChannel src,
        final Buffers buffers, final ExecutorService exec,
        final ExecutorService subex) {
        this(() -> src, buffers, exec, subex);
    }

    /**
     * Ctor.
     * @param src Source of channel
     * @param buffers Buffers allocation strategy
     * @param exec Executor service for IO operations
     * @param subex Executor service of subscriber
     */
    ReadableChannelPublisher(final ChannelSource<? extends ReadableByteChannel> src,
        final Buffers buffers, final ExecutorService exec,
        final ExecutorService subex) {
        this.src = src;
        this.buffers = buffers;
        this.exec = exec;
        this.subex = subex;
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
        final ReadSubscriberState<? super ByteBuffer> wrap =
            new ReadSubscriberState<>(new AsyncSubscriber<>(subscriber, this.subex));
        wrap.onSubscribe(
            new ReadSubscription(
                wrap, this.buffers,
                new ReadTaskQueue(wrap, chan, this.exec)
            )
        );
    }
}

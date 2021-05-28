/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.jctools.queues.SpscUnboundedArrayQueue;

/**
 * Read loop for read requests.
 * @since 0.1
 * @checkstyle NestedIfDepthCheck (500 lines)
 */
final class ReadTaskQueue implements Runnable {

    /**
     * Requests queue.
     */
    private final Queue<ReadRequest> queue;

    /**
     * Subscriber.
     */
    private final ReadSubscriberState<? super ByteBuffer> sub;

    /**
     * Channel.
     */
    private final ReadableByteChannel channel;

    /**
     * Exeutor service.
     */
    private final Executor exec;

    /**
     * Running flag.
     */
    private final AtomicBoolean running;

    /**
     * New busy loop.
     * @param sub Subscriber
     * @param channel File channel
     * @param exec Executor service to process tasks
     * @checkstyle MagicNumberCheck (10 lines)
     */
    ReadTaskQueue(final ReadSubscriberState<? super ByteBuffer> sub,
        final ReadableByteChannel channel, final Executor exec) {
        this.queue = new SpscUnboundedArrayQueue<>(128);
        this.sub = sub;
        this.exec = exec;
        this.channel = channel;
        this.running = new AtomicBoolean();
    }

    @Override
    public void run() {
        while (!this.sub.done()) {
            ReadRequest next = this.queue.poll();
            if (next == null) {
                this.running.set(false);
                if (!this.queue.isEmpty() && this.running.compareAndSet(false, true)) {
                    if (this.sub.done()) {
                        break;
                    }
                    next = this.queue.poll();
                    if (next == null) {
                        continue;
                    }
                } else {
                    return;
                }
            }
            next.process(this.channel);
        }
        if (this.channel.isOpen()) {
            try {
                this.channel.close();
            } catch (final IOException err) {
                Logger.getLogger(this.getClass().getSimpleName())
                    .warning(String.format("Failed to close channel: %s", err));
            }
        }
    }

    /**
     * Asks queue to accept read request.
     * @param request Request to accept
     */
    public void accept(final ReadRequest request) {
        if (this.sub.done()) {
            return;
        }
        this.queue.add(request);
        if (this.running.compareAndSet(false, true)) {
            this.exec.execute(
                new ErrorOnException(
                    new CloseChanOnError(this, this.channel),
                    this.sub
                )
            );
        }
    }

    /**
     * Asks queue to clear itself.
     */
    public void clear() {
        this.queue.clear();
    }
}

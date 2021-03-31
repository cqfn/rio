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

import com.jcabi.log.Logger;
import org.cqfn.rio.WriteGreed;
import org.jctools.queues.SpscUnboundedArrayQueue;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Write subscription runnable task loop.
 *
 * @since 0.1
 * @checkstyle MethodBodyCommentsCheck (500 lines)
 * @checkstyle CyclomaticComplexityCheck (500 lines)
 * @checkstyle NestedIfDepthCheck (500 lines)
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 */
final class WriteTaskQueue implements Runnable {

    /**
     * Attempts to loop for receive next request.
     */
    private static final int LOOP_ATTEMPTS = 5;

    /**
     * Target future.
     */
    private final CompletableFuture<Void> future;

    /**
     * File channel.
     */
    private final WritableByteChannel channel;

    /**
     * Subscription reference.
     */
    private final AtomicReference<Subscription> sub;

    /**
     * Request queue.
     */
    private final Queue<WriteRequest> queue;

    /**
     * Write greed level.
     */
    private final WriteGreed greed;

    /**
     * Executor service.
     */
    private final Executor exec;

    /**
     * Running atomic flag.
     */
    private final AtomicBoolean running;

    /**
     * Ctor.
     * @param future Target future
     * @param channel File channel
     * @param sub Subscription reference
     * @param greed Greed level
     * @param exec Executor service
     * @checkstyle ParameterNumberCheck (5 lines)
     * @checkstyle MagicNumberCheck (10 lines)
     */
    WriteTaskQueue(final CompletableFuture<Void> future,
        final WritableByteChannel channel, final AtomicReference<Subscription> sub,
        final WriteGreed greed, final Executor exec) {
        this.future = future;
        this.channel = channel;
        this.sub = sub;
        this.queue = new SpscUnboundedArrayQueue<>(128);
        this.greed = greed;
        this.exec = exec;
        this.running = new AtomicBoolean();
    }

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public void run() {
        int attempts = WriteTaskQueue.LOOP_ATTEMPTS;
        while (!this.future.isDone()) {
            // requesting next chunk of byte buffers according to greed strategy
            WriteRequest next = this.queue.poll();
            // if no next item, try to exit the loop
            boolean empty = next == null;

            if (empty) {
                if (--attempts > 0) {
//                    Thread.yield();
                    continue;
                }
                assert attempts == 0 : "attempt skipped";
                // mark this loop as finished
                final boolean stopped = this.running.compareAndSet(true,false);
                assert stopped : "running flag inconsistency";
                // recover - if next item available and this loop is still not running
                // continue running this loop and process it
                if (!this.queue.isEmpty() && this.running.compareAndSet(false, true)) {
                    next = this.queue.poll();
                    empty = next == null;
                    if (empty) {
                        attempts = WriteTaskQueue.LOOP_ATTEMPTS;
                        continue;
                    }
                } else {
                    // if empty or acquired by next loop - exit
                    return;
                }
            }
            assert !empty && next != null : "can't process empty or null element";
            next.process(this.channel);
            this.greed.processed(this.sub.get());
            attempts = WriteTaskQueue.LOOP_ATTEMPTS;
        }

        // future completed
        if (this.channel.isOpen()) {
            try {
                this.channel.close();
            } catch (final IOException err) {
                Logger.warn(this, "Failed to close channel: %[exception]s", err);
            }
        }
        // Optional.ofNullable(this.sub.getAndSet(null)).ifPresent(Subscription::cancel);
        this.running.set(false);
    }

    /**
     * Asks to accept write request.
     * @param req Write request
     */
    public void accept(final WriteRequest req) {
        if (this.future.isDone()) {
            return;
        }
        if (req instanceof WriteRequest.Error) {
            this.queue.clear();
        }
        this.greed.received(this.sub.get());
        this.queue.add(req);
        if (this.running.compareAndSet(false, true)) {
            this.exec.execute(this);
        }
    }

    public int size() {
        return this.queue.size();
    }
}

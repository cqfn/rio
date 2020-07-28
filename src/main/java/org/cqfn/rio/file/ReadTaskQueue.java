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

import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final FileChannel channel;

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
        final FileChannel channel, final Executor exec) {
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
                Logger.warn(this, "Failed to close channel: %[exception]s", err);
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

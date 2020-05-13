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

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Read flow subscription.
 * @since 0.1
 */
final class ReadSubscription implements Subscription {

    /**
     * Read subscriber.
     */
    private final ReadSubscriberState<? super ByteBuffer> sub;

    /**
     * Buffers allocation strategy.
     */
    private final Buffers buffers;

    /**
     * Requests queue.
     */
    private final Queue<ReadRequest> queue;

    /**
     * New read subscription.
     * @param chan File channel
     * @param out Output subscriber
     * @param buffers Buffers allocation strategy
     */
    ReadSubscription(final FileChannel chan, final Subscriber<? super ByteBuffer> out,
        final Buffers buffers) {
        this.sub = new ReadSubscriberState<>(out);
        this.buffers = buffers;
        this.queue = new ConcurrentLinkedQueue<>();
        final ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(new ShutdownOnExit(exec, new ReadBusyLoop(this.queue, this.sub, chan)));
    }

    @Override
    public void request(final long count) {
        if (this.sub.done()) {
            return;
        }
        if (count <= 0) {
            this.queue.clear();
            this.sub.onError(new IllegalArgumentException(String.format("Requested %d items", count)));
            return;
        }
        this.queue.add(new ReadRequest.Next(this.sub, this.buffers, count));
    }

    @Override
    public void cancel() {
        this.sub.cancel();
        this.queue.clear();
    }
}

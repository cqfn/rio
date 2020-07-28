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
     * Tasks queue.
     */
    private final ReadTaskQueue queue;

    /**
     * New read subscription.
     * @param sub Output subscriber
     * @param buffers Buffers allocation strategy
     * @param queue Read task queue
     */
    ReadSubscription(final ReadSubscriberState<? super ByteBuffer> sub,
        final Buffers buffers, final ReadTaskQueue queue) {
        this.sub = sub;
        this.buffers = buffers;
        this.queue = queue;
    }

    @Override
    public void request(final long count) {
        if (this.sub.done()) {
            return;
        }
        if (count <= 0) {
            this.queue.clear();
            this.sub.onError(
                new IllegalArgumentException(String.format("Requested %d items", count))
            );
        } else {
            this.queue.accept(new ReadRequest.Next(this.sub, this.buffers, count));
        }
    }

    @Override
    public void cancel() {
        this.sub.cancel();
        this.queue.clear();
    }
}

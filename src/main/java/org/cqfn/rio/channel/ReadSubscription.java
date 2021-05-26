/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

import java.nio.ByteBuffer;
import org.cqfn.rio.Buffers;
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

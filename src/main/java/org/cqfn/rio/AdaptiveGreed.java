/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */

package org.cqfn.rio;

import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptive write greed that pays attention to
 * consumers demands. It's one of the key element of the RIO
 * algorithm.
 * <p>
 * Greed object is notified on two events: on receiving chung of data
 * from the producer, and on finishing processing this chunk.
 * It requests the {@code amount} of chunks on chunks receiving
 * if the received element is a ordinary equal to {@code amount - shift},
 * let's call this element as requesting point.
 * <p>
 * This greed implementation monitors the consumer's queue size
 * and pay attention to the demand of the consumer:
 * if queue size is equal to zero on requesting point,
 * than requesting amount is increased twice and shift is increased by one
 * if it's less than {@code 3} (it's imperically tested that increasing
 * shift more than 3 doesn't give any positive performance improvement).
 * If the queue size is greater than half of amount on requesting element,
 * the size is reduced twice but no less than 3, and shift is reduced by one
 * of it's not less than 3.
 *
 * @since 0.3
 */
public final class AdaptiveGreed implements WriteGreed {

    /**
     * Amount to request.
     */
    private volatile long amount;

    /**
     * Request shift.
     * <p>
     * If shift is greater than zero, this object will request
     * next chunk before all previous chunks were consumed. E.g.
     * if it's requesting 100 items on each iteration and shift is equal to 2,
     * then next request of 100 items will occur on 98 item.
     * </p>
     */
    private volatile long shift;

    /**
     * Request counter.
     */
    private final AtomicLong cnt;

    /**
     * Queue size.
     */
    private final AtomicLong queue;

    /**
     * Adjusting flag instead of synchronisation:
     * write task queue allows only single consumer single producer operations,
     * adjusting adaptive parameters should be performed on receiving next chunk only.
     * We assert that using this flag.
     */
    private final AtomicBoolean adjusting;

    /**
     * New adaptie greed with initial values.
     *
     * @param amount Amount to request
     * @param shift  Request items before shifted amount was processed
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public AdaptiveGreed(final long amount, final long shift) {
        if (amount < 1) {
            throw new IllegalArgumentException("Amount should be greater than 1");
        }
        if (shift >= amount) {
            throw new IllegalArgumentException("Shift should be less than amount");
        }
        this.amount = amount;
        this.shift = shift;
        this.cnt = new AtomicLong();
        this.queue = new AtomicLong();
        this.adjusting = new AtomicBoolean();
    }

    @Override
    public void init(final Subscription sub) {
        sub.request(this.amount);
    }

    @Override
    public void received(final Subscription sub) {
        final long size = this.queue.getAndIncrement();
        final long pos = this.cnt.incrementAndGet();
        assert size >= 0 : "received: conumer drains more elements than produced";
        assert pos >= 0 : "position can not be negative";
        if (pos == this.amount - this.shift) {
            this.adjust(size);
            sub.request(amount);
            this.cnt.addAndGet(-pos);
        }
    }

    @Override
    public void processed(final Subscription sub) {
        long size = this.queue.decrementAndGet();
        assert size >= 0 : "processed: conumer drains more elements than produced";
    }

    /**
     * Adjust requested amount and shift.
     *
     * @param size Current queue size
     */
    private void adjust(final long size) {
        if (!this.adjusting.compareAndSet(false, true)) {
            throw new IllegalStateException("Unsupported adjusting enter overlaps");
        }

        if (size < this.shift) {
            this.amount *= 2;
            if (this.shift < 3) {
                this.shift++;
            }
        }
        if (size > this.amount && this.amount > 6) {
            this.amount /= 2;
            if (this.shift > 1) {
                this.shift--;
            }
        }
        if (!this.adjusting.compareAndSet(true, false)) {
            throw new IllegalStateException("Adjusting ended illegally");
        }
    }
}

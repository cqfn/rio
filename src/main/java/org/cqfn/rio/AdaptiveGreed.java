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

package org.cqfn.rio;

import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscription;

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
 *
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
     * New adaptie greed with initial values.
     * @param amount Amount to request
     * @param shift Request items before shifted amount was processed
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
    }

    @Override
    public void init(final Subscription sub) {
        sub.request(this.amount);
    }

    @Override
    public void received(final Subscription sub) {
        final long size = this.queue.incrementAndGet();
        final long pos = this.cnt.incrementAndGet();
        assert size >= 0 : "received: conumer drains more elements than produced";
        assert pos >= 0 : "position can not be negative";
        if (pos == this.amount - this.shift) {
            System.out.printf("WG-A: request %d (size=%d pos=%s)\n", amount, size, pos);
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
     * @param size Current queue size
     */
    private void adjust(final long size) {
        long oldAmount = this.amount;
        long oldShift = this.shift;
        if (size < this.shift) {
            synchronized (this.queue) {
                if (size < this.shift) {
                    this.amount *= 2;
                    if (this.shift < 3) {
                        this.shift++;
                    }
                }
                System.out.printf("adjusted↑ [%d(%d)] -> [%d(%d)]\n",
                        oldAmount, oldShift, this.amount, this.shift);
            }
        }
        if (size > this.amount && this.amount > 6) {
            synchronized (this.queue) {
                if (size > this.amount) {
                    this.amount /= 2;
                }
                if (this.shift > 1) {
                    this.shift--;
                }
                System.out.printf("adjusted↓ [%d(%d)] -> [%d(%d)]\n",
                        oldAmount, oldShift, this.amount, this.shift);
            }
        }
    }
}

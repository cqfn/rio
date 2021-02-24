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
 * consumers demands.
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
     * Counter.
     */
    private final AtomicLong cnt;

    /**
     * Received counter.
     */
    private final AtomicLong rec;

    /**
     * New adaptie greed with initial values.
     * @param amount Amount to request
     * @param shift Request items before shifted amount was processed
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public AdaptiveGreed(final long amount, final long shift) {
        if (shift >= amount) {
            throw new IllegalArgumentException("Shift should be less than amount");
        }
        this.amount = amount;
        this.shift = shift;
        this.cnt = new AtomicLong();
        this.rec = new AtomicLong();
    }

    @Override
    public boolean request(final Subscription sub) {
        final long pos = this.cnt.getAndIncrement();
        final boolean result = pos == 0 || pos % (this.amount - this.shift + 1) == 0;
        if (result) {
            if (this.cnt.get() - this.rec.get() < this.shift + 1) {
                this.shift *= 2;
            }
            if (this.shift > this.amount / 2) {
                this.amount *= 2;
            }
            sub.request(this.amount);
        }
        return result;
    }

    @Override
    public void received() {
        this.rec.incrementAndGet();
    }
}

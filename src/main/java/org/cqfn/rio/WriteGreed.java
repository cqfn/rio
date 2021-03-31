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
 * Greed level of write consumer.
 * @since 0.2
 */
public interface WriteGreed {

    /**
     * Request one chunk on each request.
     */
    WriteGreed SINGLE = new Constant(1L, 0L);

    /**
     * Greed level from system {@code rio.file.write.greed} property, or {@code 1} default.
     */
    WriteGreed SYSTEM = new Constant(
        Long.getLong("org.cqfn.rio.WriteGreed#amount", 3),
        Long.getLong("org.cqfn.rio.WriteGreed#shift", 1)
    );

    /**
     * Init the subscription.
     * @param sub Subscription to init
     */
    void init(Subscription sub);

    /**
     * Received next chunks from subscription.
     * @param sub Subsscription
     * @return True if reuqested successfully
     */
    void received(Subscription sub);

    /**
     * Processed chunk.
     * @param sub Subsscription
     */
    default void processed(Subscription sub) {
        // do nothing
    }

    /**
     * Try to convert into adaptive mode.
     * @return Adaptive greed if applicable.
     */
    default WriteGreed adaptive() {
        return this;
    }

    /**
     * Request always constant amount.
     * @since 0.2
     */
    final class Constant implements WriteGreed {

        /**
         * Amount to request.
         */
        private final long amount;

        /**
         * Request shift.
         * <p>
         * If shift is greater than zero, this object will request
         * next chunk before all previous chunks were consumed. E.g.
         * if it's requesting 100 items on each iteration and shift is equal to 2,
         * then next request of 100 items will occur on 98 item.
         * </p>
         */
        private final long shift;

        /**
         * Counter.
         */
        private final AtomicLong cnt;

        /**
         * New constant greed level.
         * @param amount Amount to request
         * @param shift Request items before shifted amount was processed
         */
        @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
        public Constant(final long amount, final long shift) {
            if (shift >= amount) {
                throw new IllegalArgumentException("Shift should be less than amount");
            }
            this.amount = amount;
            this.shift = shift;
            this.cnt = new AtomicLong();
        }

        @Override
        public void init(final Subscription sub) {
            sub.request(this.amount);
            System.out.println("WG-C: init");
        }

        @Override
        public void received(final Subscription sub) {
            final long pos = this.cnt.incrementAndGet();
            System.out.printf("WG-C: received %d\n", pos);
            if (pos == this.amount - this.shift) {
                sub.request(this.amount);
                this.cnt.addAndGet(-pos);
            }
        }

        @Override
        public WriteGreed adaptive() {
            return new AdaptiveGreed(this.amount, this.shift);
        }
    }
}

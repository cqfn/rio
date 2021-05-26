/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio;

import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;

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
        }

        @Override
        public void received(final Subscription sub) {
            final long pos = this.cnt.incrementAndGet();
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

/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.file;

import org.reactivestreams.Subscription;

/**
 * Greed level of write consumer.
 * @deprecated Use {@link org.cqfn.rio.WriteGreed} instead
 * @since 0.1
 */
@Deprecated
public interface WriteGreed extends org.cqfn.rio.WriteGreed {

    /**
     * Request one chunk on each request.
     */
    WriteGreed SINGLE = new Constant(1L, 0L);
    /**
     * Greed level from system {@code rio.file.write.greed} property, or {@code 1} default.
     */
    WriteGreed SYSTEM = new Constant(
        Long.getLong("rio.file.write.greed.amount", 3),
        Long.getLong("rio.file.write.greed.shift", 1)
    );

    /**
     * Request always constant amount.
     * @since 0.1
     */
    final class Constant implements WriteGreed {

        /**
         * Origin constant.
         */
        private final org.cqfn.rio.WriteGreed cst;

        /**
         * New constant greed level.
         * @param amount Amount to request
         * @param shift Request items before shifted amount was processed
         */
        public Constant(final long amount, final long shift) {
            this.cst = new org.cqfn.rio.WriteGreed.Constant(amount, shift);
        }

        @Override
        public void init(final Subscription sub) {
            this.cst.init(sub);
        }

        @Override
        public void received(final Subscription sub) {
            this.cst.received(sub);
        }

        @Override
        public void processed(final Subscription sub) {
            this.cst.processed(sub);
        }
    }
}

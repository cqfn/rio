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

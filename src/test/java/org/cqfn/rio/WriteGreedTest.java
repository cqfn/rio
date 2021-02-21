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

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;

/**
 * Test case for {@link WriteGreed}.
 *
 * @since 1.0
 * @checkstyle JavadocMethodCheck (500 lines)
 */
public final class WriteGreedTest {

    /**
     * Dummy subscription.
     */
    private static final Subscription SUB_DUMMY = new Subscription() {
        @Override
        public void request(final long items) {
            // nothing
        }

        @Override
        public void cancel() {
            // nothing
        }
    };

    @Test
    void constantGreedRespectsAmountAndShift() {
        final WriteGreed.Constant greed = new WriteGreed.Constant(3, 1);
        // @checkstyle MagicNumberCheck (1 line)
        final boolean[] results = new boolean[10];
        for (int pos = 0; pos < results.length; ++pos) {
            results[pos] = greed.request(WriteGreedTest.SUB_DUMMY);
        }
        MatcherAssert.assertThat(
            results,
            Matchers.equalTo(
                new boolean[]{
                    true, false, false,
                    true, false, false,
                    true, false, false,
                    true,
                }
            )
        );
    }
}

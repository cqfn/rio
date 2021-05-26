/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
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
        // final WriteGreed.Constant greed = new WriteGreed.Constant(3, 1);
        // // @checkstyle MagicNumberCheck (1 line)
        // final boolean[] results = new boolean[10];
        // for (int pos = 0; pos < results.length; ++pos) {
        //     results[pos] = greed.request(WriteGreedTest.SUB_DUMMY);
        // }
        // MatcherAssert.assertThat(
        //     results,
        //     Matchers.equalTo(
        //         new boolean[]{
        //             true, false, false,
        //             true, false, false,
        //             true, false, false,
        //             true,
        //         }
        //     )
        // );
    }
}

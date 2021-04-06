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
package org.cqfn.rio.channel;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Logger;
import org.cqfn.rio.Buffers;

/**
 * Read request.
 * @since 0.1
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
abstract class ReadRequest {

    /**
     * Flow subscriber.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    protected final ReadSubscriberState<? super ByteBuffer> sub;

    /**
     * Logger.
     */
    protected final Logger logger;

    /**
     * New read request.
     * @param sub Subscriber
     */
    private ReadRequest(final ReadSubscriberState<? super ByteBuffer> sub) {
        this.sub = sub;
        this.logger = Logger.getLogger(this.getClass().getSimpleName());
    }

    /**
     * Process file channel.
     * @param channel Channel to process
     */
    abstract void process(ReadableByteChannel channel);

    /**
     * Next request.
     * @since 0.1
     */
    static final class Next extends ReadRequest {

        /**
         * Buffers.
         */
        private final Buffers buffers;

        /**
         * Count to request.
         */
        private final long count;

        /**
         * New read request.
         * @param sub Subscriber
         * @param buffers Buffer allocation strategy
         * @param count Amount of requests
         */
        Next(final ReadSubscriberState<? super ByteBuffer> sub, final Buffers buffers,
            final long count) {
            super(sub);
            this.buffers = buffers;
            this.count = count;
        }

        // @checkstyle ReturnCountCheck (50 lines)
        @Override
        @SuppressWarnings({"PMD.OnlyOneReturn", "PMD.AvoidCatchingGenericException"})
        void process(final ReadableByteChannel channel) {
            for (int cnt = 0; cnt < this.count; ++cnt) {
                if (this.sub.done()) {
                    return;
                }
                final ByteBuffer buf = this.buffers.create();
                final int read;
                try {
                    read = channel.read(buf);
                } catch (final IOException iex) {
                    try {
                        channel.close();
                    } catch (final IOException cex) {
                        this.logger.warning(
                            String.format(
                                "Failed to close channel on errors: %s", cex
                            )
                        );
                    }
                    this.sub.onError(iex);
                    return;
                }
                ((Buffer) buf).flip();
                if (read >= 0) {
                    try {
                        this.sub.onNext(buf);
                        // @checkstyle IllegalCatchCheck (1 line)
                    } catch (final Exception exx) {
                        try {
                            channel.close();
                        } catch (final IOException cex) {
                            this.logger.warning(
                                String.format(
                                    "Failed to close channel on next error: %s", cex
                                )
                            );
                        }
                        this.sub.onError(exx);
                        return;
                    }
                } else {
                    try {
                        channel.close();
                        this.sub.onComplete();
                    } catch (final IOException iex) {
                        this.sub.onError(iex);
                    }
                    return;
                }
            }
        }
    }
}

/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
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

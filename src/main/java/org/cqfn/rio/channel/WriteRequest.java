/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Request to write.
 * @since 0.1
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
abstract class WriteRequest {

    /**
     * Write future.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    protected final CompletableFuture<Void> future;

    /**
     * Logger.
     */
    protected final Logger logger;

    /**
     * Ctor.
     * @param future Write future
     */
    private WriteRequest(final CompletableFuture<Void> future) {
        this.future = future;
        this.logger = Logger.getLogger(this.getClass().getSimpleName());
    }

    /**
     * Process write request.
     * @param chan Output channel
     */
    abstract void process(WritableByteChannel chan);

    /**
     * Next write request with data.
     * @since 0.1
     */
    static final class Next extends WriteRequest {

        /**
         * Target buffer.
         */
        private final ByteBuffer target;

        /**
         * New next write request.
         * @param future Future
         * @param target Buffer
         */
        Next(final CompletableFuture<Void> future, final ByteBuffer target) {
            super(future);
            this.target = target;
        }

        @Override
        public void process(final WritableByteChannel chan) {
            while (this.target.hasRemaining()) {
                try {
                    chan.write(this.target);
                } catch (final IOException iex) {
                    try {
                        chan.close();
                    } catch (final IOException cex) {
                        this.logger.warning(
                            String.format(
                                "Failed to close channel on next failure: %s", cex
                            )
                        );
                    }
                    this.future.completeExceptionally(iex);
                    return;
                }
            }
        }

        @Override
        public String toString() {
            return String.format("Write next %s", this.target);
        }
    }

    /**
     * Complete request.
     * @since 0.1
     */
    static final class Complete extends WriteRequest {

        /**
         * New complete signal.
         * @param future Write future
         */
        Complete(final CompletableFuture<Void> future) {
            super(future);
        }

        @Override
        public void process(final WritableByteChannel chan) {
            try {
                chan.close();
            } catch (final IOException iex) {
                this.future.completeExceptionally(iex);
                return;
            }
            this.future.complete(null);
        }

        @Override
        public String toString() {
            return "Complete";
        }
    }

    /**
     * Error signal.
     * @since 0.1
     */
    static final class Error extends WriteRequest {

        /**
         * Error.
         */
        private final Throwable err;

        /**
         * New error signal.
         * @param future Write future
         * @param err Error
         */
        Error(final CompletableFuture<Void> future, final Throwable err) {
            super(future);
            this.err = err;
        }

        @Override
        public String toString() {
            return String.format("Error: %s", this.err.getMessage());
        }

        @Override
        void process(final WritableByteChannel chan) {
            try {
                chan.close();
            } catch (final IOException cex) {
                this.logger.warning(
                    String.format("Failed to close channel on error: %[exception]s", cex)
                );
            }
            this.future.completeExceptionally(this.err);
        }
    }
}

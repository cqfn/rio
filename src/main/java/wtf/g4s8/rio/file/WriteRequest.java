/*
 * MIT License
 *
 * Copyright (c) 2020 g4s8
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
package wtf.g4s8.rio.file;

import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CompletableFuture;

/**
 * Request to write.
 * @since 0.1
 */
abstract class WriteRequest {

    /**
     * Write future.
     */
    protected final CompletableFuture<Void> future;

    /**
     * Ctor.
     * @param future Write future
     */
    private WriteRequest(final CompletableFuture<Void> future) {
        this.future = future;
    }

    /**
     * Process write request.
     * @param chan Output channel
     */
    abstract void process(FileChannel chan);

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
        public Next(final CompletableFuture<Void> future, final ByteBuffer target) {
            super(future);
            this.target = target;
        }

        @Override
        public void process(final FileChannel chan) {
            while (this.target.hasRemaining()) {
                try {
                    chan.write(this.target);
                } catch (final IOException iex) {
                    try {
                        chan.close();
                    } catch (final IOException cex) {
                        Logger.warn(
                            this,
                            "Failed to close channel on next failure: %[exception]s", cex
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
        public Complete(final CompletableFuture<Void> future) {
            super(future);
        }

        @Override
        public void process(final FileChannel chan) {
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
        public Error(final CompletableFuture<Void> future, final Throwable err) {
            super(future);
            this.err = err;
        }

        @Override
        void process(final FileChannel chan) {
            try {
                chan.close();
            } catch (final IOException cex) {
                Logger.warn(
                    this,
                    "Failed to close channel on error: %[exception]s", cex
                );
            }
            this.future.completeExceptionally(this.err);
        }

        @Override
        public String toString() {
            return String.format("Error: %s", this.err.getMessage());
        }
    }
}

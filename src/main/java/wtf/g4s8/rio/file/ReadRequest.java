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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Read request.
 * @since 0.1
 */
abstract class ReadRequest {

    /**
     * Flow subscriber.
     */
    protected final ReadSubscriberState<? super ByteBuffer> sub;

    /**
     * New read request.
     * @param sub Subscriber
     */
    private ReadRequest(final ReadSubscriberState<? super ByteBuffer> sub) {
        this.sub = sub;
    }

    /**
     * Process file channel.
     * @param channel Channel to process
     */
    abstract void process(final FileChannel channel);

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
         * @param count Amount of requests
         */
        Next(final ReadSubscriberState<? super ByteBuffer> sub, final Buffers buffers,
            final long count) {
            super(sub);
            this.buffers = buffers;
            this.count = count;
        }

        @Override
        void process(final FileChannel channel) {
            for (int i = 0; i < this.count; ++i) {
                if (this.sub.done()) {
                    return;
                }
                final ByteBuffer buf = this.buffers.create();
                final int read;
                try {
                    read = channel.read(buf);
                } catch (final IOException iex) {
                    this.sub.onError(iex);
                    return;
                }
                buf.flip();
                if (read >= 0) {
                    this.sub.onNext(buf);
                } else {
                    this.sub.onComplete();
                    return;
                }
            }
        }
    }
}

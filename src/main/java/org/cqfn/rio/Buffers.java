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

import java.nio.ByteBuffer;

/**
 * Read buffering strategy.
 * <p>
 * This object is requested for next buffer for read operation.
 * It's recommended to allocate new buffer on each request, but in case where memory consumption is
 * the key, single instance of {@link ByteBuffer} can be used with proper recycling, in that case
 * developer should care about all concurrency issues and be sure that this buffer is used
 * sequentially for read and write.
 * </p>
 * @since 0.2
 */
public interface Buffers {

    /**
     * Create byte buffer.
     * @return Byte buffer
     */
    ByteBuffer create();

    /**
     * Standard direct byte buffers.
     * @since 0.2
     */
    enum Standard implements Buffers {
        /**
         * The smallest possible by buffer, can be used for debugging.
         */
        MIN(1),
        /**
         * One kilobyte buffer.
         */
        K1(1024),
        /**
         * Four kilobytes buffer.
         */
        K4(K1.size * 4),
        /**
         * Eight kilobytes buffer.
         */
        K8(K1.size * 8),
        /**
         * Sixteen kilobytes buffer.
         */
        K16(K1.size * 16);

        /**
         * Buffer size.
         */
        private final int size;

        /**
         * New standard buffer.
         * @param size Buffer size
         */
        Standard(final int size) {
            this.size = size;
        }

        @Override
        public ByteBuffer create() {
            return ByteBuffer.allocateDirect(this.size);
        }
    }
}

/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
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

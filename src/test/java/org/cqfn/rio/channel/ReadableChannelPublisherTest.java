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

import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.cqfn.rio.Buffers;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

/**
 * Test case for {@link ReadChannlePublisher}.
 *
 * @since 0.1
 * @checkstyle JavadocMethodCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ReturnCountCheck (500 lines)
 */
@SuppressWarnings(
    {
        "PMD.TestClassWithoutTestCases", "PMD.OnlyOneReturn",
        "PMD.JUnit4TestShouldUseBeforeAnnotation"
    }
)
public final class ReadableChannelPublisherTest
    extends PublisherVerification<ByteBuffer> {

    /**
     * Ctor.
     */
    public ReadableChannelPublisherTest() {
        super(new TestEnvironment());
    }

    @BeforeClass
    public void setUp() {
        if (System.getProperty("os.name").toLowerCase(Locale.US).contains("win")) {
            throw new SkipException("Disabled for windows");
        }
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(final long size) {
        if (size == 0) {
            return Flowable.empty();
        }
        final int capacity = 128;
        return new ReadableChannelPublisher(
            () -> new SourceChan((int) (capacity * size)),
            () -> ByteBuffer.allocateDirect(capacity),
            Executors.newCachedThreadPool()
        );
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return new ReadableChannelPublisher(
            () -> {
                throw new IOException("test-error");
            },
            Buffers.Standard.K1,
            Executors.newCachedThreadPool()
        );
    }

    @Override
    public long maxElementsFromPublisher() {
        return 20;
    }

    @Override
    public long boundedDepthOfOnNextAndRequestRecursion() {
        return 1;
    }

    /**
     * Source channel for test with generated data.
     * @since 1.0
     */
    private static final class SourceChan extends AbstractInterruptibleChannel
        implements ReadableByteChannel {

        /**
         * Channel size.
         */
        private final AtomicInteger size;

        /**
         * New test channel.
         * @param size Channel size
         */
        SourceChan(final int size) {
            this.size = new AtomicInteger(size);
        }

        @Override
        public int read(final ByteBuffer buf) throws IOException {
            boolean success = false;
            try {
                super.begin();
                if (!super.isOpen()) {
                    success = true;
                    return -1;
                }
                final int len = Math.min(this.size.get(), buf.remaining());
                buf.put(new byte[len]);
                if (this.size.addAndGet(-len) <= 0) {
                    this.close();
                }
                success = true;
                return len;
            } finally {
                super.end(success);
            }
        }

        @Override
        public void implCloseChannel() throws IOException {
            // nothing to close
        }
    }
}

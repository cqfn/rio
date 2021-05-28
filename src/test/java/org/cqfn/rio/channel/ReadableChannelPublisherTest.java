/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.cqfn.rio.Buffers;
import org.cqfn.rio.IoExecutor;
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
            IoExecutor.shared()
        );
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return new ReadableChannelPublisher(
            () -> {
                throw new IOException("test-error");
            },
            Buffers.Standard.K1,
            IoExecutor.shared()
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

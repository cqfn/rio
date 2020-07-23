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

import io.reactivex.Flowable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.Executors;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

/**
 * Test case for {@link ReadFlow}.
 *
 * @since 0.1
 * @checkstyle JavadocMethodCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings(
    {
        "PMD.TestClassWithoutTestCases", "PMD.OnlyOneReturn",
        "PMD.JUnit4TestShouldUseBeforeAnnotation"
    }
)
public final class ReadFlowTest extends PublisherVerification<ByteBuffer> {

    /**
     * Ctor.
     */
    public ReadFlowTest() {
        super(new TestEnvironment());
    }

    @BeforeClass
    public void setUp() {
        if (System.getProperty("os.name").toLowerCase(Locale.US).contains("win")) {
            throw new SkipException("Disabled for windows");
        }
    }

    // @checkstyle ReturnCountCheck (50 lines)
    @Override
    public Publisher<ByteBuffer> createPublisher(final long size) {
        if (size == 0) {
            return Flowable.empty();
        }
        final Path tmp;
        try {
            tmp = Files.createTempFile(ReadFlowTest.class.getSimpleName(), ".test");
            tmp.toFile().deleteOnExit();
            new TestResource("file.bin").copy(tmp);
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
        final int capacity = (int) (5200 / size);
        return new ReadFlow(
            tmp,
            () -> ByteBuffer.allocateDirect(capacity),
            Executors.newCachedThreadPool()
        );
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return new ReadFlow(
            Paths.get("/some/file/which/doesnt/exist"),
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
}

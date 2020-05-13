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
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

/**
 * Test case for {@link WriteSubscriber}.
 *
 * @since 1.0
 * @checkstyle JavadocMethodCheck (500 lines)
 */
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.OnlyOneReturn"})
public final class WriteSubscriberTest extends SubscriberBlackboxVerification<ByteBuffer> {

    @BeforeClass
    public void setUp() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            throw new SkipException("Disabled for windows");
        }
    }

    /**
     * Ctor.
     */
    public WriteSubscriberTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber() {
        final Path tmp;
        try {
            tmp = Files.createTempFile(this.getClass().getSimpleName(), ".tmp");
            tmp.toFile().deleteOnExit();
            return new WriteSubscriber(
                FileChannel.open(tmp, Collections.singleton(StandardOpenOption.WRITE)),
                WriteGreed.SINGLE
            );
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }

    @Override
    public ByteBuffer createElement(final int element) {
        final byte[] arr = new byte[1024];
        Arrays.fill(arr, (byte) element);
        return ByteBuffer.wrap(arr);
    }
}

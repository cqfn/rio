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
package org.cqfn.rio.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.Executors;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

/**
 * Test case for {@link WriteSubscriber}.
 *
 * @since 1.0
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle AnonInnerLengthCheck (500 lines)
 */
@SuppressWarnings(
    {
        "PMD.TestClassWithoutTestCases", "PMD.OnlyOneReturn",
        "PMD.JUnit4TestShouldUseBeforeAnnotation"
    }
)
public final class WriteSubscriberTest extends SubscriberWhiteboxVerification<ByteBuffer> {

    /**
     * Ctor.
     */
    public WriteSubscriberTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber(
        final WhiteboxSubscriberProbe<ByteBuffer> probe
    ) {
        final Path tmp;
        try {
            tmp = Files.createTempFile(this.getClass().getSimpleName(), ".tmp");
            tmp.toFile().deleteOnExit();
            return new SubscriberWithProbe<>(
                new WriteSubscriber(
                    FileChannel.open(tmp, Collections.singleton(StandardOpenOption.WRITE)),
                    WriteGreed.SINGLE,
                    Executors.newCachedThreadPool()
                ),
                probe
            );
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }

    @BeforeClass
    public void setUp() {
        if (System.getProperty("os.name").toLowerCase(Locale.US).contains("win")) {
            throw new SkipException("Disabled for windows");
        }
    }

    @Override
    public ByteBuffer createElement(final int element) {
        final byte[] arr = new byte[1024];
        Arrays.fill(arr, (byte) element);
        return ByteBuffer.wrap(arr);
    }

    /**
     * Subscriber with probe.
     * @param <T> Subscriber type
     * @since 0.2
     */
    private static class SubscriberWithProbe<T> implements Subscriber<T> {

        /**
         * Target subscriber.
         */
        private final Subscriber<T> target;

        /**
         * Test probe.
         */
        private final WhiteboxSubscriberProbe<T> probe;

        /**
         * Ctor.
         * @param target Subscriber
         * @param probe For test
         */
        SubscriberWithProbe(final Subscriber<T> target,
            final WhiteboxSubscriberProbe<T> probe) {
            this.target = target;
            this.probe = probe;
        }

        @Override
        public void onSubscribe(final Subscription subscription) {
            this.target.onSubscribe(subscription);
            this.probe.registerOnSubscribe(new ProbePuppet(subscription));
        }

        @Override
        public void onNext(final T next) {
            this.target.onNext(next);
            this.probe.registerOnNext(next);
        }

        @Override
        public void onError(final Throwable err) {
            this.target.onError(err);
            this.probe.registerOnError(err);
        }

        @Override
        public void onComplete() {
            this.target.onComplete();
            this.probe.registerOnComplete();
        }
    }

    /**
     * Puppet for subscriber probe.
     * @since 0.2
     */
    private static class ProbePuppet implements SubscriberPuppet {

        /**
         * Actual subscription.
         */
        private final Subscription subscription;

        /**
         * New puppet.
         * @param subscription Of subscriber
         */
        ProbePuppet(final Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void triggerRequest(final long elements) {
            this.subscription.request(elements);
        }

        @Override
        public void signalCancel() {
            this.subscription.cancel();
        }
    }
}

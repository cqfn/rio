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

import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link File}.
 *
 * @since 0.1
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class FileTest {

    /**
     * Hex bytes array.
     */
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);

    @RepeatedTest(1000)
    void readsContent(@TempDir final Path tmp) throws Exception {
        final Path file = tmp.resolve("test");
        new TestResource("file.bin").copy(file);
        final String sha256 = Flowable.fromPublisher(new File(file).content(Buffers.Standard.K1)).reduceWith(
            () -> MessageDigest.getInstance("SHA-256"),
            (digest, buf) -> {
                digest.update(buf);
                return digest;
            }
        ).map(MessageDigest::digest).map(FileTest::bytesToHex).blockingGet();
        MatcherAssert.assertThat(
            sha256,
            Matchers.equalTo("064EA88A18650615410970219992D54DA5CEFAE194A23FCBE3C3AF484CB3F501")
        );
    }

    /**
     * This test is supposed to verify valid usage of {@link java.lang.ref.WeakReference}
     * in {@link ReadFlow}.
     * @param tmp Temporary folder
     * @throws Exception On error
     */
    @RepeatedTest(10)
    void readsContentWithGarbageCollection(@TempDir final Path tmp) throws Exception {
        final Path file = tmp.resolve("test");
        new TestResource("file.bin").copy(file);
        final String sha256 = Flowable.fromPublisher(new File(file).content(Buffers.Standard.K1)).reduceWith(
            () -> MessageDigest.getInstance("SHA-256"),
            (digest, buf) -> {
                digest.update(buf);
                Thread.sleep(10);
                System.gc();
                return digest;
            }
        ).map(MessageDigest::digest).map(FileTest::bytesToHex).blockingGet();
        MatcherAssert.assertThat(
            sha256,
            Matchers.equalTo("064EA88A18650615410970219992D54DA5CEFAE194A23FCBE3C3AF484CB3F501")
        );
    }

    @RepeatedTest(1000)
    void writeFile(@TempDir final Path tmp) throws Exception {
        final Path out = tmp.resolve("out.bin");
        new File(out).write(
            Flowable.generate(new WriteTestSource(1024, 10))
        ).thenRun(
            () -> {
                try {
                    MatcherAssert.assertThat(
                        bytesToHex(sha256().digest(Files.readAllBytes(out))),
                        Matchers.equalTo("AFF310597FAB48A0CE2689C8221AF0D86D50E2A06B5D9CA6838A3FBF6754CC23")
                    );
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        ).toCompletableFuture().get();
    }

    @RepeatedTest(1000)
    void writeByteByByte(@TempDir final Path tmp) throws Exception {
        final String hello = "hello-world!!!";
        final Path target = tmp.resolve("saved.txt");
        new File(target).write(
            Flowable.fromArray(FileTest.boxed(hello.getBytes(StandardCharsets.UTF_8))).map(
                bte -> {
                    final byte[] bytes = new byte[1];
                    bytes[0] = bte;
                    return ByteBuffer.wrap(bytes);
                }
            )
        ).toCompletableFuture().get();
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(target), StandardCharsets.UTF_8),
            Matchers.equalTo(hello)
        );
    }

    @RepeatedTest(1000)
    void copy(@TempDir final Path tmp) throws Exception {
        final Path src = tmp.resolve("source");
        final Path dest = tmp.resolve("dst");
        new TestResource("file.bin").copy(src);
        new File(dest).write(new File(src).content(Buffers.Standard.K1)).toCompletableFuture().get();
        MatcherAssert.assertThat(
            bytesToHex(sha256().digest(Files.readAllBytes(dest))),
            Matchers.equalTo("064EA88A18650615410970219992D54DA5CEFAE194A23FCBE3C3AF484CB3F501")
        );
    }

    @Test
    void copySingleThread(@TempDir final Path tmp) throws Exception {
        final Path src = tmp.resolve("source");
        final Path dest = tmp.resolve("dst");
        new TestResource("file.bin").copy(src);
        final ExecutorService exec = Executors.newSingleThreadExecutor();
        new File(dest).write(new File(src).content(Buffers.Standard.K16, exec), exec)
            .toCompletableFuture().get();
        MatcherAssert.assertThat(
            bytesToHex(sha256().digest(Files.readAllBytes(dest))),
            Matchers.equalTo("064EA88A18650615410970219992D54DA5CEFAE194A23FCBE3C3AF484CB3F501")
        );
    }

    @Test
    @Timeout(2)
    @EnabledIfSystemProperty(named = "test.huge", matches = "true")
    void writeHugeFile(@TempDir final Path tmp) throws Exception {
        final Path target = tmp.resolve("target");
        new File(target).write(
            Flowable.generate(new WriteTestSource(1024, 1024 * 1024))
        ).toCompletableFuture().get();
    }

    /**
     * New SHA256 digest.
     * @return Message digest
     */
    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException err) {
            throw new IllegalStateException("No sha256 digest", err);
        }
    }

    /**
     * Convert bytes to hex.
     * @param bytes Bytes
     * @return Hex string
     */
    private static String bytesToHex(final byte[] bytes) {
        final byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = FileTest.HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = FileTest.HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    /**
     * Convert primitive bytes to objects.
     * @param src Primitive array
     * @return Boxed array
     */
    private static Byte[] boxed(final byte[] src) {
        final Byte[] res = new Byte[src.length];
        for (int pos = 0; pos < src.length; ++pos) {
            res[pos] = src[pos];
        }
        return res;
    }

    /**
     * Provider of byte buffers for write test.
     * @since 0.1
     */
    private static final class WriteTestSource implements Consumer<Emitter<ByteBuffer>> {

        /**
         * Counter.
         */
        private final AtomicInteger cnt;

        /**
         * Amount of buffers.
         */
        private final int length;

        /**
         * Buffer size.
         */
        private final int size;

        /**
         * New test source.
         * @param size Buffer size
         * @param length Amount of buffers
         */
        WriteTestSource(final int size, final int length) {
            this.cnt = new AtomicInteger();
            this.size = size;
            this.length = length;
        }

        @Override
        public void accept(final Emitter<ByteBuffer> src) {
            final int val = this.cnt.getAndIncrement();
            if (val < this.length) {
                final byte[] data = new byte[this.size];
                Arrays.fill(data, (byte) val);
                src.onNext(ByteBuffer.wrap(data));
            } else {
                src.onComplete();
            }
        }
    }
}

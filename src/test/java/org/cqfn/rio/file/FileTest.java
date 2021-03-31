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

import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.cqfn.rio.Buffers;
import org.cqfn.rio.WriteGreed;
import org.cqfn.rio.ext.BufferSource;
import org.cqfn.rio.ext.BufferSourceExtension;
import org.cqfn.rio.ext.TestResource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;

/**
 * Test case for {@link File}.
 *
 * @since 0.1
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
@ExtendWith(BufferSourceExtension.class)
public final class FileTest {

    /**
     * Hex bytes array.
     */
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);

    @RepeatedTest(1000)
    void readsContent(@TempDir final Path tmp) throws Exception {
        final Path file = tmp.resolve("test");
        new TestResource("file.bin").copy(file);
        final String sha = Flowable.fromPublisher(
            new File(file).content(Buffers.Standard.K1)
        ).reduceWith(
            () -> MessageDigest.getInstance("SHA-256"),
            (digest, buf) -> {
                digest.update(buf);
                return digest;
            }
        ).map(MessageDigest::digest).map(FileTest::bytesToHex).blockingGet();
        MatcherAssert.assertThat(
            sha,
            Matchers.equalTo(
                "064EA88A18650615410970219992D54DA5CEFAE194A23FCBE3C3AF484CB3F501"
            )
        );
    }

    @RepeatedTest(1000)
    void writeFile(@TempDir final Path tmp,
        @BufferSource(buffers = 10) final Publisher<ByteBuffer> source) throws Exception {
        final Path out = tmp.resolve("out.bin");
        new File(out).write(source).thenRun(
            () -> {
                try {
                    MatcherAssert.assertThat(
                        bytesToHex(sha256().digest(Files.readAllBytes(out))),
                        Matchers.equalTo(
                            "84FF92691F909A05B224E1C56ABB4864F01B4F8E3C854E4BB4C7BAF1D3F6D652"
                        )
                    );
                } catch (final IOException err) {
                    throw new IllegalStateException(err);
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
        new File(dest)
            .write(new File(src).content(Buffers.Standard.K1)).toCompletableFuture().get();
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
        new File(dest, exec).write(new File(src, exec).content(Buffers.Standard.K1))
            .toCompletableFuture().get();
        MatcherAssert.assertThat(
            bytesToHex(sha256().digest(Files.readAllBytes(dest))),
            Matchers.equalTo("064EA88A18650615410970219992D54DA5CEFAE194A23FCBE3C3AF484CB3F501")
        );
    }

    @Test
    void requestNextItemsOnlyOnDemand(@TempDir final Path tmp,
        @BufferSource(buffers = 100) final Publisher<ByteBuffer> source) throws Exception {
        new File(tmp.resolve("output1")).write(
            source, new WriteGreed.Constant(1, 0)
        ).toCompletableFuture().get();
    }

    @Test
    @Timeout(2)
    @EnabledIfSystemProperty(named = "test.hugeFiles", matches = "true|yes|on|1")
    void writeHugeFile(@TempDir final Path tmp,
        @BufferSource(buffers = 1024 * 1024) final Publisher<ByteBuffer> source) throws Exception {
        final Path target = tmp.resolve("target");
        new File(target).write(source, WriteGreed.SYSTEM).toCompletableFuture().get();
    }

    @Test
    @Timeout(2)
    @EnabledIfSystemProperty(named = "test.hugeFiles", matches = "true|yes|on|1")
    void writeAndReadHugeFile(@TempDir final Path tmp,
        @BufferSource(buffers = 1024 * 1024) final Publisher<ByteBuffer> source) throws Exception {
        final Path target = tmp.resolve("target");
        new File(target).write(source, WriteGreed.SYSTEM).toCompletableFuture().get();
        final long size = Flowable.fromPublisher(new File(target).content())
            .reduce(0L, (acc, buf) -> acc + buf.remaining()).blockingGet();
        MatcherAssert.assertThat(size, Matchers.equalTo(1024 * 1024 * 1024L));
    }

    /**
     * New SHA256 digest.
     * @return Message digest
     * @checkstyle MethodNameCheck (5 lines)
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
        final byte[] hex = new byte[bytes.length * 2];
        for (int pos = 0; pos < bytes.length; ++pos) {
            final int bte = bytes[pos] & 0xFF;
            hex[pos * 2] = FileTest.HEX_ARRAY[bte >>> 4];
            hex[pos * 2 + 1] = FileTest.HEX_ARRAY[bte & 0x0F];
        }
        return new String(hex, StandardCharsets.UTF_8);
    }

    /**
     * Convert primitive bytes to objects.
     * @param src Primitive array
     * @return Boxed array
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private static Byte[] boxed(final byte[] src) {
        final Byte[] res = new Byte[src.length];
        for (int pos = 0; pos < src.length; ++pos) {
            res[pos] = src[pos];
        }
        return res;
    }
}

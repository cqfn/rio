/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.file;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;

/**
 * Tests with RxJava flowables.
 * @since 0.1
 */
public final class FileWithRxJavaTest {

    @Test
    void shouldSave(@TempDir final Path tmp) throws Exception {
        final byte[] data = "0".getBytes();
        final File file = new File(tmp.resolve("save"));
        file.write(Flowable.just(ByteBuffer.wrap(data)))
            .toCompletableFuture().get();
        MatcherAssert.assertThat(
            readFully(file.content()),
            Matchers.equalTo(data)
        );
    }

    @Test
    void shouldSaveFromMultipleBuffers(@TempDir final Path tmp) throws Exception {
        final File file = new File(tmp.resolve("saveFromMultipleBuffers"));
        file.write(
            Flowable.fromArray(
                ByteBuffer.wrap("12".getBytes()),
                ByteBuffer.wrap("34".getBytes()),
                ByteBuffer.wrap("5".getBytes())
            )
        ).toCompletableFuture().get();
        MatcherAssert.assertThat(
            readFully(file.content()),
            Matchers.equalTo("12345".getBytes())
        );
    }

    @Test
    void shouldSaveEmpty(@TempDir final Path tmp) throws Exception {
        final File file = new File(tmp.resolve("shouldSaveEmpty"));
        file.write(Flowable.empty()).toCompletableFuture().get();
        MatcherAssert.assertThat(
            readFully(file.content()),
            Matchers.equalTo(new byte[0])
        );
    }

    @Test
    void shouldSaveWhenValueAlreadyExists(@TempDir final Path tmp) throws Exception {
        final byte[] original = "1".getBytes();
        final byte[] updated = "2".getBytes();
        final File file = new File(tmp.resolve("shouldSaveWhenValueAlreadyExists"));
        file.write(Flowable.just(ByteBuffer.wrap(original))).toCompletableFuture().get();
        file.write(Flowable.just(ByteBuffer.wrap(updated))).toCompletableFuture().get();
        MatcherAssert.assertThat(
            readFully(file.content()),
            Matchers.equalTo(updated)
        );
    }

    @Test
    void shouldFailToSaveErrorContent(@TempDir final Path tmp) {
        Assertions.assertThrows(
            Exception.class,
            () -> new File(
                tmp.resolve("shouldFailToSaveErrorContent")
            ).write(Flowable.error(new IllegalStateException()))
                .toCompletableFuture().get()
        );
    }

    @Test
    void shouldFailToLoadAbsentValue(@TempDir final Path tmp) {
        final File file = new File(tmp.resolve("shouldFailToLoadAbsentValue"));
        Assertions.assertThrows(RuntimeException.class, () -> readFully(file.content()));
    }

    private static byte[] readFully(final Publisher<ByteBuffer> pub) {
        return Flowable.fromPublisher(pub).reduce(
            ByteBuffer.allocate(0),
            FileWithRxJavaTest::concat
        ).map(FileWithRxJavaTest::remaining).blockingGet();
    }

    private static byte[] remaining(final ByteBuffer buf) {
        final byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }

    private static ByteBuffer concat(final ByteBuffer left, final ByteBuffer right) {
        left.mark();
        right.mark();
        final ByteBuffer concat = ByteBuffer.allocate(
            left.remaining() + right.remaining()
        ).put(left).put(right);
        left.reset();
        right.reset();
        concat.flip();
        return concat;
    }
}

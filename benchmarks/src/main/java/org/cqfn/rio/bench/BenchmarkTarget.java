package org.cqfn.rio.bench;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Benchmark target provider.
 */
public interface BenchmarkTarget {

    /**
     * Read file as a stream.
     * @param path File path
     * @return Stream of byte buffers
     */
    Publisher<ByteBuffer> read(Path path);

    /**
     * Write a stream to file.
     * @param path File path
     * @param data String of byte buffers
     * @return Future
     */
    CompletableFuture<?> write(Path path, Publisher<ByteBuffer> data);
}

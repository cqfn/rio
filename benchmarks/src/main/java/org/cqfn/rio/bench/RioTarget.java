package org.cqfn.rio.bench;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.cqfn.rio.file.File;
import org.reactivestreams.Publisher;

/**
 * Rio benchmark target impl.
 */
public final class RioTarget implements BenchmarkTarget {

    @Override
    public Publisher<ByteBuffer> read(final Path path) {
        return new File(path).content();
    }

    @Override
    public CompletableFuture<?> write(final Path path, final Publisher<ByteBuffer> data) {
        return new File(path)
            .write(data, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            .toCompletableFuture();
    }
}

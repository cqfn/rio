package org.cqfn.rio.bench;

import com.artipie.asto.fs.VertxRxFile;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Vertx benchmark test provider.
 */
public final class VertxTarget implements BenchmarkTarget {

    private final Vertx vertx = Vertx.vertx();

    @Override
    public Publisher<ByteBuffer> read(final Path path) {
        return new VertxRxFile(path, this.vertx).flow();
    }

    @Override
    public CompletableFuture<?> write(final Path path, final Publisher<ByteBuffer> data) {
        return new VertxRxFile(path, this.vertx)
            .save(Flowable.fromPublisher(data))
            .to(CompletableInterop.await())
            .toCompletableFuture();
    }
}

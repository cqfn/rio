package org.cqfn.rio.bench;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
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
        return this.vertx.fileSystem()
            .rxOpen(path.toString(), new OpenOptions().setRead(true))
            .flatMapPublisher(
                file -> {
                    final Promise<Void> promise = Promise.promise();
                    final Completable completable = Completable.create(
                        emitter ->
                            promise.future().onComplete(
                                event -> {
                                    if (event.succeeded()) {
                                        emitter.onComplete();
                                    } else {
                                        emitter.onError(event.cause());
                                    }
                                }
                            )
                    );
                    return file.toFlowable().map(
                        buffer -> ByteBuffer.wrap(buffer.getBytes())
                    ).doOnTerminate(() -> file.rxClose().subscribe(promise::complete))
                        .mergeWith(completable);
                }
            );
    }

    @Override
    public CompletableFuture<?> write(final Path path, final Publisher<ByteBuffer> data) {
        return this.vertx.fileSystem().rxOpen(
            path.toString(),
            new OpenOptions()
                .setCreate(true)
                .setWrite(true)
                .setTruncateExisting(true)
        )
            .flatMapCompletable(
                file -> Completable.create(
                    emitter -> Flowable.fromPublisher(data)
                        .map(buf -> Buffer.buffer(bytes(buf)))
                        .onErrorResumeNext(
                            thr -> {
                                return file.rxClose().andThen(Flowable.error(thr));
                            }
                        )
                        .subscribe(file.toSubscriber()
                            .onWriteStreamEnd(emitter::onComplete)
                            .onWriteStreamError(emitter::onError)
                            .onWriteStreamEndError(emitter::onError)
                            .onError(emitter::onError)
                        )
                )
            ).to(CompletableInterop.await()).toCompletableFuture();
    }

    private static byte[] bytes(final ByteBuffer buf) {
        final byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }
}

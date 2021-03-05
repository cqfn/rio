package org.cqfn.rio.bench;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import io.vertx.core.file.CopyOptions;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

/**
 * Vertx benchmark test provider.
 */
public final class VertxTarget implements BenchmarkTarget {

    private final Vertx vertx = Vertx.vertx();

    @Override
    public Publisher<ByteBuffer> read(final Path path) {
        return this.vertx.fileSystem().rxOpen(
            path.toString(),
            new OpenOptions()
                .setRead(true)
                .setWrite(false)
                .setCreate(false)
        )
            .flatMapPublisher(
                asyncFile -> {
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
                    return asyncFile.toFlowable().map(
                        buffer -> ByteBuffer.wrap(buffer.getBytes())
                    ).doOnTerminate(() -> asyncFile.rxClose().subscribe(promise::complete))
                        .mergeWith(completable);
                }
            );
    }

    @Override
    public CompletableFuture<?> write(final Path path, final Publisher<ByteBuffer> data) {
        return this.vertx.fileSystem().rxOpen(
            path.toString(),
            new OpenOptions()
                .setRead(false)
                .setCreate(true)
                .setWrite(true)
                .setTruncateExisting(true)
        )
            .flatMapCompletable(
                asyncFile -> Completable.create(
                    emitter -> {
                        Flowable.fromPublisher(data).map(buf -> Buffer.buffer(bts(buf)))
                            .onErrorResumeNext(
                                thr -> {
                                    return asyncFile.rxClose().andThen(Flowable.error(thr));
                                }
                            )
                            .subscribe(asyncFile.toSubscriber()
                                .onWriteStreamEnd(emitter::onComplete)
                                .onWriteStreamError(emitter::onError)
                                .onWriteStreamEndError(emitter::onError)
                                .onError(emitter::onError)
                            );
                    }
                )
            ).to(CompletableInterop.await()).toCompletableFuture();
    }

    private static byte[] bts(ByteBuffer bb) {
        final byte[] arr = new byte[bb.remaining()];
        bb.get(arr);
        return arr;
    }

}

/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.bench;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

/**
 * Spring webflux target.
 */
public class FluxTarget implements BenchmarkTarget {

    private final DataBufferFactory buffers;

    public FluxTarget() {
        this(new DefaultDataBufferFactory());
    }

    public FluxTarget(DataBufferFactory buffers) {
        this.buffers = buffers;
    }

    @Override
    public Publisher<ByteBuffer> read(Path path) {
        return DataBufferUtils.read(path, this.buffers, 1024 * 8, StandardOpenOption.READ)
                .map(DataBuffer::asByteBuffer);
    }

    @Override
    public CompletableFuture<?> write(Path path, Publisher<ByteBuffer> data) {
        return DataBufferUtils.write(
                Flux.from(data).map(this.buffers::wrap), path, StandardOpenOption.WRITE, StandardOpenOption.CREATE
        ).toFuture();
    }
}

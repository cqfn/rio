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

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import org.cqfn.rio.Buffers;
import org.cqfn.rio.IoExecutor;
import org.cqfn.rio.WriteGreed;
import org.cqfn.rio.channel.ReadableChannel;
import org.cqfn.rio.channel.WritableChannel;
import org.reactivestreams.Publisher;

/**
 * Reactive file API.
 * @since 0.1
 */
public final class File {

    /**
     * File path.
     */
    private final Path path;

    /**
     * IO executor.
     */
    private final ExecutorService exec;

    /**
     * New file.
     * @param path Path
     */
    public File(final Path path) {
        this(path, IoExecutor.shared());
    }

    /**
     * New file.
     * @param path Path
     * @param exec Executor service
     */
    public File(final Path path, final ExecutorService exec) {
        this.path = path;
        this.exec = exec;
    }

    /**
     * File's content.
     * @return Content publisher
     */
    public Publisher<ByteBuffer> content() {
        return this.content(Buffers.Standard.K8);
    }

    /**
     * File's content.
     * @param buf Buffers policy
     * @return Content publisher
     */
    public Publisher<ByteBuffer> content(final Buffers buf) {
        return new ReadableChannel(
            () -> FileChannel.open(this.path, StandardOpenOption.READ),
            this.exec
        ).read(buf);
    }

    /**
     * Write data to file.
     * @param data Data publisher
     * @param opts Options
     * @return Future
     */
    public CompletionStage<Void> write(final Publisher<ByteBuffer> data, final OpenOption... opts) {
        return this.write(data, WriteGreed.SYSTEM.adaptive(), opts);
    }

    /**
     * Write data to file.
     * @param data Data publisher
     * @param greed Greed level of consumer
     * @param opts Options
     * @return Future
     * @checkstyle ParameterNumberCheck (7 lines)
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    public CompletionStage<Void> write(final Publisher<ByteBuffer> data,
        final WriteGreed greed, final OpenOption... opts) {
        return new WritableChannel(
            () -> FileChannel.open(this.path, writeOpts(opts)),
            this.exec
        ).write(data, greed);
    }

    /**
     * Write options.
     * @param src User specified options
     * @return Fixed options
     */
    private static OpenOption[] writeOpts(final OpenOption... src) {
        final OpenOption[] opts;
        if (src.length == 0) {
            opts = new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE};
        } else {
            opts = src;
        }
        return opts;
    }
}

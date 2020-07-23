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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.reactivestreams.Publisher;

/**
 * Reactive file API.
 * @since 0.1
 */
public final class File {

    /**
     * Default executor.
     */
    private static final ExecutorService EXEC_DEFAULT = Executors.newCachedThreadPool();

    /**
     * File path.
     */
    private final Path path;

    /**
     * New file.
     * @param path Path
     */
    public File(final Path path) {
        this.path = path;
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
        return this.content(buf, File.EXEC_DEFAULT);
    }

    /**
     * File's content.
     * @param exec Executor service to perform IO operations
     * @return Content publisher
     */
    public Publisher<ByteBuffer> content(final ExecutorService exec) {
        return new ReadFlow(this.path, Buffers.Standard.K8, exec);
    }

    /**
     * File's content.
     * @param buf Buffers policy
     * @param exec Executor service to perform IO operations
     * @return Content publisher
     */
    public Publisher<ByteBuffer> content(final Buffers buf, final ExecutorService exec) {
        return new ReadFlow(this.path, buf, exec);
    }

    /**
     * Write data to file.
     * @param data Data publisher
     * @param opts Options
     * @return Future
     */
    public CompletionStage<Void> write(final Publisher<ByteBuffer> data, final OpenOption... opts) {
        return this.write(data, WriteGreed.SYSTEM, opts);
    }

    /**
     * Write data to file.
     * @param data Data publisher
     * @param exec Executor service to perform IO operations
     * @param opts Options
     * @return Future
     */
    public CompletionStage<Void> write(final Publisher<ByteBuffer> data,
        final ExecutorService exec,
        final OpenOption... opts) {
        return this.write(data, WriteGreed.SYSTEM, exec, opts);
    }

    /**
     * Write data to file.
     * @param data Data publisher
     * @param greed Greed level of consumer
     * @param opts Options
     * @return Future
     */
    public CompletionStage<Void> write(final Publisher<ByteBuffer> data, final WriteGreed greed,
        final OpenOption... opts) {
        return this.write(data, greed, File.EXEC_DEFAULT, opts);
    }

    /**
     * Write data to file.
     * @param data Data publisher
     * @param greed Greed level of consumer
     * @param exec Executor service to perform IO operations
     * @param opts Options
     * @return Future
     * @checkstyle ReturnCountCheck (20 lines)
     * @checkstyle ParameterNumberCheck (7 lines)
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    public CompletionStage<Void> write(final Publisher<ByteBuffer> data,
        final WriteGreed greed,
        final ExecutorService exec,
        final OpenOption... opts) {
        final WriteSubscriber sub;
        try {
            sub = new WriteSubscriber(
                FileChannel.open(
                    this.path,
                    new HashSet<>(Arrays.asList(writeOpts(opts)))
                ),
                greed,
                exec
            );
        } catch (final IOException err) {
            final CompletableFuture<Void> res = new CompletableFuture<>();
            res.completeExceptionally(err);
            return res;
        }
        data.subscribe(sub);
        return sub;
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

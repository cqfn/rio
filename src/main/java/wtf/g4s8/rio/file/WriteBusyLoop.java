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

import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscription;

/**
 * Write subscription busy loop runnable.
 * @since 0.1
 */
final class WriteBusyLoop implements Runnable {

    /**
     * Target future.
     */
    private final CompletableFuture<Void> future;

    /**
     * File channel.
     */
    private final FileChannel channel;

    /**
     * Subscription reference.
     */
    private final AtomicReference<Subscription> sub;

    /**
     * Request queue.
     */
    private final Queue<WriteRequest> queue;

    /**
     * Write greed level.
     */
    private final WriteGreed greed;

    /**
     * Ctor.
     * @param future Target future
     * @param channel File channel
     * @param sub Subscription reference
     * @param queue Request queue
     * @param greed Greed level
     * @checkstyle ParameterNumberCheck (5 lines)
     */
     WriteBusyLoop(final CompletableFuture<Void> future, final FileChannel channel,
        final AtomicReference<Subscription> sub, final Queue<WriteRequest> queue,
        final WriteGreed greed) {
        this.future = future;
        this.channel = channel;
        this.sub = sub;
        this.queue = queue;
        this.greed = greed;
    }

    @Override
    public void run() {
        while (!this.future.isDone()) {
            this.greed.request(this.sub.get());
            final WriteRequest next = this.queue.poll();
            if (next == null) {
                continue;
            }
            next.process(this.channel);
        }
        if (this.channel.isOpen()) {
            try {
                this.channel.close();
            } catch (final IOException err) {
                Logger.warn(this, "Failed to close channel: %[exception]s", err);
            }
        }
        this.sub.getAndSet(null).cancel();
    }
}

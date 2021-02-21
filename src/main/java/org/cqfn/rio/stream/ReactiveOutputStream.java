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

package org.cqfn.rio.stream;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.concurrent.CompletionStage;
import org.cqfn.rio.WriteGreed;
import org.cqfn.rio.channel.WritableChannel;
import org.reactivestreams.Publisher;

/**
 * Reactive write methods for {@link OutputStream}.
 * @since 0.2
 */
public final class ReactiveOutputStream {

    /**
     * Output stream destination.
     */
    private final OutputStream src;

    /**
     * Extend {@link OutputStream} with reactive write methods.
     * @param src Output stream
     */
    public ReactiveOutputStream(final OutputStream src) {
        this.src = src;
    }

    /**
     * Write data reactively from publisher into output stream.
     * @param data Publisher to write
     * @param greed Of write consumer
     * @return Future
     */
    public CompletionStage<Void> write(final Publisher<ByteBuffer> data, final WriteGreed greed) {
        return new WritableChannel(() -> Channels.newChannel(this.src))
            .write(data, greed);
    }
}

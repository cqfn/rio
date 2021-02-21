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
package org.cqfn.rio.channel;

import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * Runnable decorator which closes channel on exit.
 * @since 0.1
 */
final class CloseChanOnError implements Runnable {

    /**
     * Decorated job to run.
     */
    private final Runnable origin;

    /**
     * Channel to close.
     */
    private final ReadableByteChannel chan;

    /**
     * Wraps runnable with close channel action on exit.
     * @param origin Runnable to decorate
     * @param chan Channel to close
     */
    CloseChanOnError(final Runnable origin, final ReadableByteChannel chan) {
        this.origin = origin;
        this.chan = chan;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public void run() {
        try {
            this.origin.run();
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Throwable err) {
            this.close();
            throw err;
        }
    }

    /**
     * Close channel.
     */
    private void close() {
        if (this.chan.isOpen()) {
            try {
                this.chan.close();
            } catch (final IOException err) {
                Logger.warn(this, "Failed to close channel: %[exception]s", err);
            }
        }
    }
}

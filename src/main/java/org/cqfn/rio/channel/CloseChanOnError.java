/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Logger;

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
                Logger.getLogger(this.getClass().getSimpleName())
                    .warning(String.format("Failed to close channel: %s", err));
            }
        }
    }
}

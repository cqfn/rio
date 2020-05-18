package wtf.g4s8.rio.file;

import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Runnable decorator which closes channel on exit.
 * @since 0.1
 */
final class CloseChanOnError implements Runnable {

    private final Runnable origin;

    private final FileChannel chan;

    CloseChanOnError(final Runnable origin, final FileChannel chan) {
        this.origin = origin;
        this.chan = chan;
    }

    @Override
    public void run() {
        try {
            this.origin.run();
        } catch (final Throwable err) {
            this.close();
            throw err;
        }
    }

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
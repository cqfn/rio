package wtf.g4s8.rio.file;

import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Runnable decorator which closes channel on exit.
 * @since 0.1
 */
final class CloseChanOnExit implements Runnable {

    private final Runnable origin;
    private final FileChannel chan;

    CloseChanOnExit(final Runnable origin, final FileChannel chan) {
        this.origin = origin;
        this.chan = chan;
    }

    @Override
    public void run() {
        try {
            this.origin.run();
        } finally {
            this.close();
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

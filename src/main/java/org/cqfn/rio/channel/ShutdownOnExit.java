/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

import java.util.concurrent.ExecutorService;

/**
 * Runnable decorator which shutdowns {@link ExecutorService} on exit.
 * @since 0.1
 */
final class ShutdownOnExit implements Runnable {

    /**
     * Executor service.
     */
    private final ExecutorService exec;

    /**
     * Origin runnable.
     */
    private final Runnable origin;

    /**
     * Wrap origin runnable.
     * @param origin Origin runnable
     * @param exec Executor to shutdown
     */
    ShutdownOnExit(final Runnable origin, final ExecutorService exec) {
        this.exec = exec;
        this.origin = origin;
    }

    @Override
    public void run() {
        try {
            this.origin.run();
        } finally {
            this.exec.shutdown();
        }
    }
}

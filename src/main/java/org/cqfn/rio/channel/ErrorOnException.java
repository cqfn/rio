/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.channel;

/**
 * Handle all exceptions including unchecked and signal error state to
 * subscriber.
 * @since 0.1
 */
final class ErrorOnException implements Runnable {

    /**
     * Origin runnable.
     */
    private final Runnable runnable;

    /**
     * Subscriber.
     */
    private final ReadSubscriberState<?> sub;

    /**
     * Wrap runnable.
     * @param runnable Runnable to wrap
     * @param sub Subscriber
     */
    ErrorOnException(final Runnable runnable, final ReadSubscriberState<?> sub) {
        this.runnable = runnable;
        this.sub = sub;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public void run() {
        try {
            this.runnable.run();
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Throwable exx) {
            this.sub.onError(exx);
        }
    }
}

/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */

package org.cqfn.rio;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standard IO executor.
 * @since 0.3
 */
public final class IoExecutor extends AbstractExecutorService {

    /**
     * Default shared instance cache.
     */
    private static volatile ExecutorService shr;

    /**
     * Origin service.
     */
    private final ExecutorService origin;

    /**
     * Default constructor.
     */
    IoExecutor() {
        this(new Factory("rio"));
    }

    /**
     * New IO executor with thread factory.
     * @param factory For new threads
     */
    IoExecutor(final ThreadFactory factory) {
        this(
            Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                factory
            )
        );
    }

    /**
     * Primary ctor.
     * @param origin Executor
     */
    IoExecutor(final ExecutorService origin) {
        this.origin = origin;
    }

    @Override
    public void execute(final Runnable arg) {
        this.origin.execute(arg);
    }

    @Override
    public boolean awaitTermination(final long time, final TimeUnit unit)
        throws InterruptedException {
        return this.origin.awaitTermination(time, unit);
    }

    @Override
    public boolean isShutdown() {
        return this.origin.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.origin.isTerminated();
    }

    @Override
    public void shutdown() {
        this.origin.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return this.origin.shutdownNow();
    }

    /**
     * Shared executor service.
     * @return Shared instance
     */
    @SuppressWarnings({"PMD.ProhibitPublicStaticMethods", "PMD.DoubleCheckedLocking"})
    public static ExecutorService shared() {
        if (IoExecutor.shr == null) {
            synchronized (IoExecutor.class) {
                if (IoExecutor.shr == null) {
                    IoExecutor.shr = new IoExecutor();
                }
            }
        }
        return IoExecutor.shr;
    }

    /**
     * Factory for IO threads.
     * @since 0.3
     */
    private static final class Factory implements ThreadFactory {

        /**
         * Thread prefix.
         */
        private final String prefix;

        /**
         * Thread name counter.
         */
        private final AtomicInteger cnt;

        /**
         * New factory for thread with prefix names.
         * @param prefix Name prefix
         */
        Factory(final String prefix) {
            this.prefix = prefix;
            this.cnt = new AtomicInteger();
        }

        @Override
        public Thread newThread(final Runnable run) {
            final Thread thr = new Thread(run);
            thr.setName(String.format("%s-%d", this.prefix, this.cnt.getAndIncrement()));
            return thr;
        }
    }
}

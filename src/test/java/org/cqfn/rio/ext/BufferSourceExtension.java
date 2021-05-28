/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.ext;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import org.junit.jupiter.api.extension.*;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Buffer source extension for {@link BufferSource}.
 *
 * @since 0.2
 */
public final class BufferSourceExtension implements ParameterResolver {

    @Override
    public boolean supportsParameter(final ParameterContext param, final ExtensionContext ext) {
        return param.isAnnotated(BufferSource.class);
    }

    @Override
    public Object resolveParameter(final ParameterContext param, final ExtensionContext ext)
            throws ParameterResolutionException {
        final Class<?> type = param.getParameter().getType();
        if (!Publisher.class.isAssignableFrom(type)) {
            throw new ExtensionConfigurationException(
                    "@BufferSource can resolve Publisher<ByteBuffer> params only"
            );
        }
        return BufferSourceExtension.publisher(
                param.findAnnotation(BufferSource.class).orElseThrow(
                        () -> new IllegalArgumentException(
                                "Parameter was not annotation with @BufferSource"
                        )
                )
        );
    }

    /**
     * Create publisher for configuration.
     *
     * @param config Configuration annotation
     * @return Publisher
     */
    private static Publisher<ByteBuffer> publisher(final BufferSource config) {
        final ByteBuffer source = ByteBuffer.allocateDirect(config.bufferSize());
        for (int pos = 0; pos < config.bufferSize(); ++pos) {
            source.put(pos, config.value());
        }
        return Flowable.create(new BufferSourceGenerator(source, config.buffers(), config.delay()), BackpressureStrategy.ERROR);
    }

    /**
     * Provider of byte buffers for write test.
     *
     * @since 0.1
     */
    private static final class BufferSourceGenerator implements FlowableOnSubscribe<ByteBuffer> {

        /**
         * Source buffer.
         */
        private final ByteBuffer source;

        /**
         * Amount of buffers.
         */
        private final AtomicLong cnt;

        /**
         * Delay ms.
         */
        private final long delay;

        /**
         * New test source.
         *
         * @param source Source buffer
         * @param size   Amount of buffers
         * @param delay  Delay on generate in millis
         */
        BufferSourceGenerator(final ByteBuffer source, final long size, long delay) {
            this.source = source;
            this.cnt = new AtomicLong(size);
            this.delay = delay;
        }

        @Override
        public void subscribe(FlowableEmitter<ByteBuffer> emitter) throws Exception {
            while (!emitter.isCancelled()) {
                for (long req = emitter.requested(); req > 0; req--) {
                    final long val = this.cnt.decrementAndGet();
                    if (val >= 0L) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(this.delay);
                        } catch (final InterruptedException ignore) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        emitter.onNext(this.source.slice());
                    } else {
                        emitter.onComplete();
                        return;
                    }
                }
            }
        }
    }
}

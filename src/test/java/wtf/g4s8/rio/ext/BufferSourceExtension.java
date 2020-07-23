package wtf.g4s8.rio.ext;

import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.reactivestreams.Publisher;

/**
 *
 * @since
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
            param.findAnnotation(BufferSource.class)
                .orElseThrow(() -> new IllegalArgumentException("Parameter was not annotation with @BufferSource"))
        );
    }

    private static Publisher<ByteBuffer> publisher(final BufferSource config) {
        final ByteBuffer source = ByteBuffer.allocateDirect(config.bufferSize());
        for (int pos = 0; pos < config.bufferSize(); ++pos) {
            source.put(pos, config.value());
        }
        return Flowable.generate(new BufferSourceGenerator(source, config.buffers()));
    }

    /**
     * Provider of byte buffers for write test.
     * @since 0.1
     */
    private static final class BufferSourceGenerator implements Consumer<Emitter<ByteBuffer>> {

        /**
         * Source buffer.
         */
        private final ByteBuffer source;

        /**
         * Amount of buffers.
         */
        private final AtomicLong cnt;

        /**
         * New test source.
         * @param source Source buffer
         * @param size Amount of buffers
         */
        BufferSourceGenerator(final ByteBuffer source, final long size) {
            this.source = source;
            this.cnt = new AtomicLong(size);
        }

        @Override
        public void accept(final Emitter<ByteBuffer> src) {
            final long val = this.cnt.decrementAndGet();
            if (val >= 0L) {
                src.onNext(this.source.slice());
            } else {
                src.onComplete();
            }
        }
    }
}

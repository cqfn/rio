package wtf.g4s8.rio.ext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @since
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BufferSource {

    /**
     * Buffer byte value.
     * @return Byte to fill the buffer
     */
    byte value() default 0x00;

    /**
     * Buffer size.
     * @return Size
     */
    int bufferSize() default 1024;

    /**
     * Amount of buffers.
     * @return Buffers to produce
     */
    long buffers();
}

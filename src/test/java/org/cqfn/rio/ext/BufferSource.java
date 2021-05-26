/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.ext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Buffer source Junit extension annotation.
 * <p>
 * Use this annotation for {@link org.reactivestreams.Publisher}
 * parameters to generate reactive byte-buffer sources.
 * </p>
 * @since 0.2
 * @checkstyle MagicNumberCheck (500 lines)
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

    /**
     * Genrator delay.
     * @return Delay in millis
     */
    long delay() default 0;
}

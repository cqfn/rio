/*
 * MIT License
 *
 * Copyright (c) 2020 cqfn.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights * to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package org.cqfn.rio.ext;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Test resource data.
 * @since 0.1
 * @checkstyle MagicNumberCheck (500 lines)
 */
public final class TestResource {

    /**
     * Resource name.
     */
    private final String name;

    /**
     * Classloader.
     */
    private final ClassLoader clo;

    /**
     * New test resource with current thread class loader.
     * @param name Name
     */
    public TestResource(final String name) {
        this(name, Thread.currentThread().getContextClassLoader());
    }

    /**
     * New test resource.
     * @param name Name
     * @param clo Class loader
     */
    public TestResource(final String name, final ClassLoader clo) {
        this.name = name;
        this.clo = clo;
    }

    /**
     * Copy resource to path.
     * @param out Path
     * @throws IOException On copy error
     */
    public void copy(final Path out) throws IOException {
        if (!Files.exists(out)) {
            Files.createFile(out);
        }
        try (
            InputStream res = new BufferedInputStream(
                Objects.requireNonNull(this.clo.getResourceAsStream(this.name))
            );
            OutputStream ous = new BufferedOutputStream(
                Files.newOutputStream(out, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            )
        ) {
            final byte[] buf = new byte[8192];
            for (int read = res.read(buf); read >= 0; read = res.read(buf)) {
                ous.write(buf, 0, read);
            }
        }
    }
}

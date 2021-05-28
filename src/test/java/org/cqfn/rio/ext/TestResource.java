/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
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

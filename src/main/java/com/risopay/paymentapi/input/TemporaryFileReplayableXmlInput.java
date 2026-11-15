package com.risopay.paymentapi.input;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replayable XML input backed by one temporary file owned by the request
 * filter. The file is deleted when the request completes.
 */
public final class TemporaryFileReplayableXmlInput
        implements ReplayableXmlInput {

    private final Path file;
    private final long sizeInBytes;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TemporaryFileReplayableXmlInput(Path file, long sizeInBytes) {
        this.file = file;
        this.sizeInBytes = sizeInBytes;
    }

    @Override
    public InputStream openStream() throws IOException {
        if (closed.get()) {
            throw new IOException("XML request input is no longer available.");
        }

        return Files.newInputStream(file);
    }

    @Override
    public long sizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            Files.deleteIfExists(file);
        }
    }
}

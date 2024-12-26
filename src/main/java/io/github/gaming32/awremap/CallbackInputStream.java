package io.github.gaming32.awremap;

import org.jetbrains.annotations.NotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongConsumer;

public class CallbackInputStream extends FilterInputStream {
    private final LongConsumer callback;
    private long read = 0L;
    private long mark;

    public CallbackInputStream(InputStream in, LongConsumer callback) {
        super(in);
        this.callback = callback;
    }

    @Override
    public int read() throws IOException {
        final var result = super.read();
        if (result != -1) {
            callback.accept(++read);
        }
        return result;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        final var read = super.read(b, off, len);
        if (read > 0) {
            callback.accept(this.read += read);
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        final var skipped = super.skip(n);
        if (skipped > 0) {
            callback.accept(read += skipped);
        }
        return skipped;
    }

    @Override
    public void mark(int readLimit) {
        super.mark(readLimit);
        mark = read;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        read = mark;
    }
}

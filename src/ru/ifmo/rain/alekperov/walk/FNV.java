package ru.ifmo.rain.alekperov.walk;

import java.io.IOException;
import java.io.InputStream;

public class FNV {

    private static final int FNV_32_PRIME = 0x01000193;
    private static final int FNV_32_INIT = 0x811c9dc5;

    public static int get32BitHash(final InputStream is) throws IOException {
        int hash = FNV_32_INIT;
        final var buffer = new byte[1024];
        int c;
        while ((c = is.read(buffer)) != -1) {
            for (int i = 0; i < c; ++i) {
                hash *= FNV_32_PRIME;
                hash ^= Byte.toUnsignedInt(buffer[i]);
            }
        }
        return hash;
    }

}
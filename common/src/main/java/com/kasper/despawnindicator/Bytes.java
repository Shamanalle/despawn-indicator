package com.kasper.despawnindicator;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * The byte format of the messages: Minecraft's VarInt (7 bits a byte, low bits first), zigzag for numbers that
 * may be negative, and strings as a VarInt byte length followed by UTF-8. The same on every loader and on Bukkit.
 */
public final class Bytes {

    private Bytes() {
    }

    /** Appends to a growing array. */
    public static final class Writer {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        public Writer varInt(int value) {
            while ((value & ~0x7F) != 0) {
                out.write((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            out.write(value);
            return this;
        }

        /** A number that may be negative, in as few bytes as a small positive one. */
        public Writer zigzag(int value) {
            return varInt((value << 1) ^ (value >> 31));
        }

        public Writer unsignedByte(int value) {
            out.write(value & 0xFF);
            return this;
        }

        public Writer string(String value) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            varInt(bytes.length);
            out.write(bytes, 0, bytes.length);
            return this;
        }

        public byte[] toByteArray() {
            return out.toByteArray();
        }
    }

    /** Reads what {@link Writer} wrote; any malformed input throws {@link IllegalArgumentException}. */
    public static final class Reader {
        private final byte[] data;
        private int pos;

        public Reader(byte[] data) {
            this.data = data;
        }

        public int varInt() {
            int value = 0;
            for (int shift = 0; shift < 35; shift += 7) {
                int b = unsignedByte();
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    return value;
                }
            }
            throw new IllegalArgumentException("VarInt too long");
        }

        public int zigzag() {
            int v = varInt();
            return (v >>> 1) ^ -(v & 1);
        }

        public int unsignedByte() {
            if (pos >= data.length) {
                throw new IllegalArgumentException("Message ends too early");
            }
            return data[pos++] & 0xFF;
        }

        public String string(int maxBytes) {
            int length = varInt();
            if (length < 0 || length > maxBytes || length > data.length - pos) {
                throw new IllegalArgumentException("Bad string length " + length);
            }
            String s = new String(data, pos, length, StandardCharsets.UTF_8);
            pos += length;
            return s;
        }

        public boolean hasMore() {
            return pos < data.length;
        }
    }
}

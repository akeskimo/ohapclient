package com.opimobi.ohap.message;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by hhedberg on 30.3.2015.
 */
public class OutgoingMessage {

    /**
     * The internal buffer. It will be grown if the message do not fit to it.
     */
    private byte[] buffer = new byte[256];

    /**
     * The position where the next byte should be appended. The initial position
     * skips the space reserved for the message length.
     */
    private int position = 2;

    /**
     * Character set used to convert strings.
     */
    private final Charset charset = Charset.forName("UTF-8");


    public OutgoingMessage integer8(int value) {
        ensureCapacity(1);
        buffer[position] = (byte)value;
        position += 1;

        return this;
    }

    public OutgoingMessage integer16(int value) {
        ensureCapacity(2);
        buffer[position] = (byte)(value >> 8);
        buffer[position + 1] = (byte)value;
        position += 2;

        return this;
    }

    public OutgoingMessage integer32(long value) {
        ensureCapacity(4);
        buffer[position] = (byte)(value >> 24);
        buffer[position + 1] = (byte)(value >> 16);
        buffer[position + 2] = (byte)(value >> 8);
        buffer[position + 3] = (byte)value;
        position += 4;

        return this;
    }

    public OutgoingMessage decimal64(double value) {
        long bits = Double.doubleToRawLongBits(value);
        ensureCapacity(8);
        buffer[position] = (byte)(bits >> 56);
        buffer[position + 1] = (byte)(bits >> 48);
        buffer[position + 2] = (byte)(bits >> 40);
        buffer[position + 3] = (byte)(bits >> 32);
        buffer[position + 4] = (byte)(bits >> 24);
        buffer[position + 5] = (byte)(bits >> 16);
        buffer[position + 6] = (byte)(bits >> 8);
        buffer[position + 7] = (byte)bits;
        position += 8;

        return this;
    }

    public OutgoingMessage allBytes(byte[] bytes) {
        ensureCapacity(bytes.length);
        System.arraycopy(bytes, 0, buffer, position, bytes.length);
        position += bytes.length;

        return this;
    }

    public OutgoingMessage text(String string) {
        byte[] b = string.getBytes(charset);
        integer16(b.length);
        allBytes(b);

        return this;
    }

    public OutgoingMessage binary8(boolean value) {
        return integer8(value ? 1 : 0);
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        int length = position;
        position = 0;
        integer16(length - 2);

        outputStream.write(buffer, 0, length);
    }

    /**
     * Ensures that the internal buffer have room for the specified amount of
     * bytes. Grows the buffer when needed by doubling its size.
     *
     * @param appendLength the amount of bytes to be appended
     */
    private void ensureCapacity(int appendLength) {
        if (position + appendLength < buffer.length)
            return;

        int newLength = buffer.length * 2;
        while (position + appendLength >= newLength)
            newLength *= 2;
        buffer = Arrays.copyOf(buffer, newLength);
    }
}



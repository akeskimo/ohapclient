package com.opimobi.ohap.message;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;

/**
 * Created by hhedberg on 30.3.2015.
 */
public class IncomingMessage {

    /**
     * The internal buffer. It is reserved in the readExactly() method.
     */
    private byte[] buffer;

    /**
     * The position where the next byte should be taken from.
     */
    private int position;

    /**
     * Character set used to convert strings.
     */
    private final Charset charset = Charset.forName("UTF-8");

    public void readFrom(InputStream inputStream) throws IOException {
        buffer = readExactly(inputStream, 2);
        position = 0;
        int length = integer16();

        buffer = readExactly(inputStream, length);
        position = 0;
    }

    public int integer8() {
        if (position + 1 > buffer.length)
            throw new ArrayIndexOutOfBoundsException();

        int value = buffer[position] & 0xff;
        position += 1;

        return value;
    }

    public int integer16() {
        if (position + 2 > buffer.length)
            throw new ArrayIndexOutOfBoundsException();

        int value = (buffer[position] & 0xff) << 8 |
                    (buffer[position + 1] & 0xff);
        position += 2;

        return value;
    }

    public long integer32() {
        if (position + 4 > buffer.length)
            throw new ArrayIndexOutOfBoundsException();

        long value = (buffer[position] & 0xffL) << 24 |
                     (buffer[position + 1] & 0xffL) << 16 |
                     (buffer[position + 2] & 0xffL) << 8 |
                     (buffer[position + 3] &0xffL);
        position += 4;

        return value;
    }

    public double decimal64() {
        if (position + 8 > buffer.length)
            throw new ArrayIndexOutOfBoundsException();

        long value = (buffer[position] & 0xffL) << 56 |
                (buffer[position + 1] & 0xffL) << 48 |
                (buffer[position + 2] & 0xffL) << 40 |
                (buffer[position + 3] & 0xffL) << 32 |
                (buffer[position + 4] & 0xffL) << 24 |
                (buffer[position + 5] & 0xffL) << 16 |
                (buffer[position + 6] & 0xffL) << 8 |
                (buffer[position + 7] &0xffL);
        position += 8;

        return Double.longBitsToDouble(value);
    }

    public void allBytes(byte[] bytes) {
        if (position + bytes.length > buffer.length)
            throw new ArrayIndexOutOfBoundsException();

        System.arraycopy(buffer, position, bytes, 0, bytes.length);
        position += bytes.length;
    }

    public boolean binary8() {
        int i = integer8();
        if (i != 0 && i != 1)
            throw new IllegalStateException("The byte was not binary.");
        return i == 1;
    }

    public String text() {
        int length = integer16();
        byte[] bytes = new byte[length];
        allBytes(bytes);

        return new String(bytes, charset);
    }

    /**
     * Reads the specified amount of bytes from the given InputStream.
     *
     * @param inputStream the InputStream from which the bytes are read
     * @param length the amount of bytes to be read
     * @return the byte array of which length is the given length
     * @throws java.io.IOException when the actual read throws an exception
     */
    private static byte[] readExactly(InputStream inputStream, int length) throws IOException, SocketTimeoutException {
        byte[] bytes = new byte[length];

        int offset = 0;
        while (length > 0) {
            int got = inputStream.read(bytes, offset, length);
            if (got == -1)
                throw new EOFException("End of message input.");
            offset += got;
            length -= got;
        }

        return bytes;
    }
}

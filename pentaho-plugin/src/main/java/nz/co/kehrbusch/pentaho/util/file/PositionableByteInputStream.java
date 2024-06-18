package nz.co.kehrbusch.pentaho.util.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class PositionableByteInputStream extends ByteArrayInputStream {
    public PositionableByteInputStream(InputStream inputStream) {
        super(toByteArray(inputStream));
    }

    public long position() {
        return pos;
    }

    public void position(long newPosition) throws IOException {
        if (newPosition < 0 || newPosition > count) {
            throw new IOException("Position out of bounds");
        }
        this.pos = (int) newPosition;
    }

    private static byte[] toByteArray(InputStream inputStream) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert ByteArrayInputStream to byte array", e);
        }
    }

    public int read(ByteBuffer dst) throws IOException {
        if (!dst.hasRemaining()) return 0;

        int remaining = dst.remaining();
        int bytesToRead = Math.min(remaining, available());
        if (bytesToRead == 0) return -1;

        byte[] buffer = new byte[bytesToRead];
        int bytesRead = read(buffer, 0, bytesToRead);
        if (bytesRead == -1) return -1;

        dst.put(buffer, 0, bytesRead);
        return bytesRead;
    }
}

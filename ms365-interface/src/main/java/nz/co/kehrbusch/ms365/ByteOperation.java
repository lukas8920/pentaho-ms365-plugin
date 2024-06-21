package nz.co.kehrbusch.ms365;

import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class ByteOperation {
    public static byte[] appendByteArraysWithComparison(byte[] array1, byte[] array2, IGraphClientDetails iGraphClientDetails) {
        String str1 = new String(array1, StandardCharsets.UTF_8);
        String str2 = new String(array2, StandardCharsets.UTF_8);

        String[] lines1 = str1.split("\n");
        String[] lines2 = str2.split("\n");

        // Compare the first lines
        if (lines1.length > 0 && lines2.length > 0 && lines1[0].equals(lines2[0])) {
            // Skip the first line of the second array
            str2 = String.join("\n", Arrays.copyOfRange(lines2, 1, lines2.length));
        }

        String resultStr = str1 + str2;
        return resultStr.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] readInputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {

            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }

            return byteArrayOutputStream.toByteArray();
        } catch (Exception e){

        } finally {
            byteArrayOutputStream.close();
        }
        return buffer;
    }
}

package top.focess.minecraft.minecraftnativesdownloader.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha1Util {

    private static final MessageDigest SHA_1;

    static {
        try {
            SHA_1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a file 's sha1 hash code.
     * @param file file
     * @return sha1 hash code of this file
     * @throws IOException if file doesn't or other IOException
     */
    public static String genSha1(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, SHA_1);
        byte[] bytes = new byte[1024];
        // read all file content
        while (digestInputStream.read(bytes) != -1);

        byte[] resultByteArry = SHA_1.digest();
        return bytesToHexString(resultByteArry);
    }

    /**
     * Convert a array of byte to hex String. <br/>
     * Each byte is covert a two character of hex String. That is <br/>
     * if byte of int is less than 16, then the hex String will append <br/>
     * a character of '0'.
     *
     * @param bytes array of byte
     * @return hex String represent the array of byte
     */
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int value = b & 0xFF;
            if (value < 16) {
                // if value less than 16, then it's hex String will be only
                // one character, so we need to append a character of '0'
                sb.append("0");
            }
            sb.append(Integer.toHexString(value));
        }
        return sb.toString();
    }
}

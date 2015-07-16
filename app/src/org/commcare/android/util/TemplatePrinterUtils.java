package org.commcare.android.util;

import android.text.TextUtils;

import org.commcare.android.crypt.CryptUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Various utilities used by TemplatePrinterTask and TemplatePrinterActivity
 * 
 * @author Richard Lu
 * @author amstone
 */
public abstract class TemplatePrinterUtils {

    private static final String FORMAT_REGEX_WITH_DELIMITER = "((?<=%2$s)|(?=%1$s))";
    private static final SecretKey KEY = CryptUtil.generateSemiRandomKey();


    /**
     * Returns a copy of the byte array, truncated to the specified length.
     *
     * @param array Input array
     * @param length Length to truncate to; must be less than or equal to array.length
     * @return Copied, truncated array
     */
    public static byte[] copyOfArray(byte[] array, int length) {
        byte[] result = new byte[length];

        for (int i = 0; i < result.length; i++) {
            result[i] = array[i];
        }

        return result;
    }

    /**
     * Gets the file extension from the given file path.
     *
     * @param path File path
     * @return File extension
     */
    public static String getExtension(String path) {
        return last(path.split("\\."));
    }

    /**
     * Concatenate all Strings in a String array to one String.
     *
     * @param strings String array to join
     * @return Joined String
     */
    public static String join(String[] strings) {
        return TextUtils.join("", strings);
    }

    /**
     * Get the last element of a String array.
     *
     * @param strings String array
     * @return Last element
     */
    public static String last(String[] strings) {
        return strings[strings.length - 1];
    }

    /**
     * Remove all occurrences of the specified String segment
     * from the input String.
     *
     * @param input String input to remove from
     * @param toRemove String segment to remove
     * @return input with all occurrences of toRemove removed
     */
    public static String remove(String input, String toRemove) {
        return TextUtils.join("", input.split(toRemove));
    }

    /**
     * Split a String while keeping the specified start and end delimiters.
     * <p/>
     * Sources:
     * http://stackoverflow.com/questions/2206378/how-to-split-a-string-but-also-keep-the-delimiters
     *
     * @param input String to split
     * @param delimiterStart Start delimiter; will split immediately before this delimiter
     * @param delimiterEnd End delimiter; will split immediately after this delimiter
     * @return Split string array
     */
    public static String[] splitKeepDelimiter(
            String input,
            String delimiterStart,
            String delimiterEnd) {

        String delimiter = String.format(FORMAT_REGEX_WITH_DELIMITER, delimiterStart, delimiterEnd);

        return input.split(delimiter);
    }

    /**
     * @param file the input file
     * @return A string representation of the entire contents of the file
     * @throws IOException
     */
    public static String docToString(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader in = new BufferedReader(new FileReader(file));
        String str;
        while ((str = in.readLine()) != null) {
            builder.append(str);
        }
        in.close();
        return builder.toString();
    }

    /**
     * Writes the given string, encrypted, to the file location specified
     *
     * @param fileText the content to write
     * @param outputPath the path of the file to write to
     * @throws IOException
     */
    public static void writeStringToFile(String fileText, String outputPath) throws IOException {
        try {
            Cipher encrypter = Cipher.getInstance("AES");
            encrypter.init(Cipher.ENCRYPT_MODE, KEY);
            FileOutputStream fos = new FileOutputStream(new File(outputPath));
            CipherOutputStream cos = new CipherOutputStream(fos, encrypter);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(cos));
            out.write(fileText);
            out.close();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads back from the encrypted file generated by the above and returns a string
     * representation of the file's contents
     *
     * @param readFromPath the path to the file from which to read
     * @return a string representation of the file's contents
     */
    public static String readStringFromFile(String readFromPath) throws IOException {
        try {
            Cipher decrypter = Cipher.getInstance("AES");
            decrypter.init(Cipher.DECRYPT_MODE, KEY);
            FileInputStream fis = new FileInputStream(new File(readFromPath));
            CipherInputStream cis = new CipherInputStream(fis, decrypter);
            BufferedReader reader = new BufferedReader(new InputStreamReader(cis));
            String docString = "";
            String line;
            while ((line = reader.readLine()) != null) {
                docString += line;
            }
            reader.close();
            return docString;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


}
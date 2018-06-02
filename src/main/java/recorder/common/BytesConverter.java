package recorder.common;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;


public class BytesConverter {
    
    private BytesConverter() {
        
    }

    public static Object objectFromBytes(byte[] bytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            Object o;
            try (ObjectInputStream in = new ObjectInputStream(bais)) {
                o = in.readObject();
            }
            return o;
        } catch (ClassNotFoundException | IOException ex) {
            throw new RuntimeException("Error during converting from bytes.", ex);
        }
    }

    public static byte[] objectToBytes(Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
                out.writeObject(object);
            }
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Error during converting to bytes.", ex);
        }
    }

    public static void fileFromBytes(byte[] bytes, File file) {
        if (file == null) {
            throw new NullPointerException();
        }
        try {
            Files.write(file.toPath(), bytes);
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Can't write bytes to file \"%s\".", file.getAbsolutePath()), ex);
        }
    }

    public static byte[] fileToBytes(File file) {
        if (file == null) {
            throw new NullPointerException();
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Can't read bytes from file \"%s\".", file.getAbsolutePath()), ex);
        }
        return bytes;
    }
    
    public static String bytesToString(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    public static byte[] bytesFromString(String string) {
        try {
            return Base64.getDecoder().decode(string);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Error during converting from bytes.", ex);
        }
    }
}

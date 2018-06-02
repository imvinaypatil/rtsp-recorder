package recorder.common;


public final class FileNameFunstions {

    private FileNameFunstions() {
    }

    public static String withoutExtension(String string) {
        return string.substring(0, string.lastIndexOf('.'));
    }

    public static String extension(String string) {
        return string.substring(string.lastIndexOf('.') + 1);
    }

    public static String randomName() {
        return RandomStringGenerator.generate();
    }
}

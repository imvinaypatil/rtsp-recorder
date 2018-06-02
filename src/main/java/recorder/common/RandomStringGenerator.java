package recorder.common;

import java.util.Random;


public final class RandomStringGenerator {

    private RandomStringGenerator() {
    }

    private static final char[] SYMBOLS;

    /**
     * It seems there is no 100% guarantee that Random is thread-safe on all
     * RJEs.
     */
    private static final ThreadLocal<Random> RANDOM_HOLDER = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }

    };

    static {
        StringBuilder tmp = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch) {
            tmp.append(ch);
        }
        for (char ch = 'a'; ch <= 'z'; ++ch) {
            tmp.append(ch);
        }
        SYMBOLS = tmp.toString().toCharArray();
    }

    public static String generate(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length < 1: " + length);
        }
        char[] buf = new char[length];
        for (int idx = 0; idx < buf.length; idx++) {
            buf[idx] = SYMBOLS[RANDOM_HOLDER.get().nextInt(SYMBOLS.length)];
        }

        return new String(buf);
    }

    public static String generate() {
        return generate(26);
    }
}

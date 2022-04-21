package gatlingdemostore;

import java.util.concurrent.ThreadLocalRandom;

public class SessionId {

    private static final char[] CANDIDATES =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int SESSION_ID_LENGTH = 10;

    static String random() {
        StringBuilder buffer = new StringBuilder(SESSION_ID_LENGTH);
        for (int i = 0; i < SESSION_ID_LENGTH; i++) {
            buffer.append(CANDIDATES[ThreadLocalRandom.current().nextInt(CANDIDATES.length)]);
        }
        return buffer.toString();
    }
}

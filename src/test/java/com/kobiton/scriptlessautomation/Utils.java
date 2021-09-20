package com.kobiton.scriptlessautomation;

public class Utils {
    public static <T> T retry(Task<T> task, int maxAttempts, int intervalInMs) throws Exception {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return task.exec(attempt);
            } catch (Exception e) {
                task.handleException(e, attempt);
                if (attempt == maxAttempts) throw e;
            }

            if (intervalInMs > 0) Thread.sleep(intervalInMs);
        }

        return null;
    }

    public static String convertToOrdinal(int i) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];
        }
    }

    public abstract static class Task<T> {
        abstract T exec(int attempt) throws Exception;

        public void handleException(Exception e, int attempt) throws Exception {
            // Default impl: do nothing
        }
    }
}

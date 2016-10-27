package cz.pikadorama.simpleorm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Tomas on 1.9.2015.
 */
public class Strings {

    private static final String DEFAULT_DELIMITER = ",";

    private Strings() {}

    /**
     * Join strings with the given delimiter.
     *
     * @param strings list of strings to join
     * @param delimiter delimiter
     * @return strings joined by the given delimiter
     * @throws IllegalArgumentException in case anz parameter is null
     */
    public static String join(List<String> strings, String delimiter) {
        if (strings == null || delimiter == null) {
            throw new IllegalArgumentException("List of strings to join and delimiter are mandatory.");
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(strings.get(i));
        }
        return builder.toString();
    }

    /**
     * Join strings with default delimiter which is a comma.
     *
     * @param strings list of strings to join
     * @return strings joined by comma
     */
    public static String join(List<String> strings) {
        return join(strings, DEFAULT_DELIMITER);
    }

    /**
     * Splits the given string with the delimiter and returns list of parts.
     *
     * @param string string to split
     * @param delimiter delimiter to use
     * @return list of parts
     */
    public static List<String> split(String string, String delimiter) {
        if (string == null || delimiter == null) {
            throw new IllegalArgumentException("Strings and delimiter are mandatory.");
        }

        if (string.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(Arrays.asList(string.split(delimiter)));
    }

    /**
     * Splits the given string with the default delimiter and returns list of parts.
     *
     * @param string string to split
     * @return list of parts
     */
    public static List<String> split(String string) {
        return split(string, DEFAULT_DELIMITER);
    }

    /**
     * Produces string like ?,?,?,..,? for SQL query placeholders.
     *
     * @param length number of question marks
     * @return string in the SQL query placeholder format
     */
    public static String makeSqlPlaceholders(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("No SQL placeholders to create. Specify length > 0.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    public static String trim(CharSequence sequence) {
        int start = 0;
        while (start < sequence.length() && Character.isWhitespace(sequence.charAt(start))) {
            start++;
        }

        int end = sequence.length();
        while (end > start && Character.isWhitespace(sequence.charAt(end - 1))) {
            end--;
        }

        return sequence.subSequence(start, end).toString();
    }

    public static String trim(String string) {
        return string == null ? null : string.trim();
    }

}

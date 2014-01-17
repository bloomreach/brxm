package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.regex.Pattern;

/**
 * @version "$Id$"
 */
public final class VersionUtils {


    private static final Pattern VERSION_PATTERN = Pattern.compile("[\\._\\-]");
    private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile("\\d+");

    private VersionUtils() {
    }

    //VersionUtils.compareVersionNumbers("1.00.00, 1.00.01) >= 0;
    @SuppressWarnings("CallToStringCompareTo")
    public static int compareVersionNumbers(CharSequence version1, CharSequence version2) {
        if (version1 == null && version2 == null) {
            return 0;
        } else if (version1 == null) {
            return -1;
        } else if (version2 == null) {
            return 1;
        }

        String[] part1 = VERSION_PATTERN.split(version1);
        String[] part2 = VERSION_PATTERN.split(version2);

        int idx = 0;
        for (; idx < part1.length && idx < part2.length; idx++) {
            String p1 = part1[idx];
            String p2 = part2[idx];

            int cmp;
            if (VERSION_NUMBER_PATTERN.matcher(p1).matches() && VERSION_NUMBER_PATTERN.matcher(p2).matches()) {
                cmp = Integer.valueOf(p1).compareTo(Integer.valueOf(p2));
            } else {
                cmp = part1[idx].compareTo(part2[idx]);
            }
            if (cmp != 0) {
                return cmp;
            }
        }

        if (part1.length == part2.length) {
            return 0;
        } else if (part1.length > idx) {
            return 1;
        } else {
            return -1;
        }
    }
}

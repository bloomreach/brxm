package org.onehippo.cms7.essentials.dashboard.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @version "$Id: VersionUtilsTest.java 174288 2013-08-19 16:21:19Z mmilicevic $"
 */
public class VersionUtilsTest {


    @Test
    public void testCompareVersionNumbers() throws Exception {
        int result = VersionUtils.compareVersionNumbers("1.02.01", "1.02.00");
        assertEquals(1, result);
        result = VersionUtils.compareVersionNumbers("1.02.00", "1.02.00");
        assertEquals(0, result);
        result = VersionUtils.compareVersionNumbers("1.02.00", "1.02.01");
        assertEquals(-1, result);
        //############################################
        // SNAPSHOT
        //############################################
        result = VersionUtils.compareVersionNumbers("1.02.01-SNAPSHOT", "1.02.00");
        assertEquals(1, result);
        result = VersionUtils.compareVersionNumbers("1.02.00-SNAPSHOT", "1.02.00-SNAPSHOT");
        assertEquals(0, result);
        result = VersionUtils.compareVersionNumbers("1.02.00", "1.02.01-SNAPSHOT");
        assertEquals(-1, result);
        result = VersionUtils.compareVersionNumbers(null, null);
        assertEquals(0, result);
        result = VersionUtils.compareVersionNumbers("1.00.00", null);
        assertEquals(1, result);
        result = VersionUtils.compareVersionNumbers(null, "1.00.00");
        assertEquals(-1, result);

    }
}

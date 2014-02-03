package org.onehippo.cms7.essentials.dashboard.utils.update;

import org.junit.Assert;
import org.junit.Test;

public class UpdateConfigTest {

    public static final String NAME = "name";
    public static final String SCRIPT = "println echo";
    public static final String QUERY = "//element(*,hippo:document)";
    public static final long BATCH_SIZE = 10l;
    public static final long THROTTLE = 10l;
    public static final boolean DRY_RUN = false;
    private UpdateUtils.UpdateConfig config;

    @Test
    public void testParams() throws Exception {
        config = new UpdateUtils.UpdateConfig(NAME, SCRIPT, QUERY, BATCH_SIZE, THROTTLE, DRY_RUN);
        Assert.assertEquals(NAME, config.getName());
        Assert.assertEquals(SCRIPT, config.getScript());
        Assert.assertEquals(QUERY, config.getQuery());
        Assert.assertEquals(BATCH_SIZE, config.getBatchSize());
        Assert.assertEquals(THROTTLE, config.getThrottle());
        Assert.assertEquals(DRY_RUN, config.isDryRun());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionOnEmptyName() throws Exception {
        config = new UpdateUtils.UpdateConfig("", SCRIPT, QUERY, BATCH_SIZE, THROTTLE, DRY_RUN);
    }

}

package org.hippoecm.hst.content.beans.standard;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static junit.framework.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class HippoResourceTest {

    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(HippoResourceTest.class);
    private HippoResource resource;

    @Before
    public void setUp() throws Exception {
        //resource = new MockHippoResource(1024);
    }

    @Test
    public void testGetLengthKB() throws Exception {
        resource = new MockHippoResource(1024);
        assertTrue("Expected 0,125 KB, but got: " + resource.getLengthKB(), resource.getLengthKB().equals(new BigDecimal("0.125")));
        resource = new MockHippoResource(1024 * 8);
        assertTrue("Expected 1 KB, but got: " + resource.getLengthKB(), resource.getLengthKB().equals(new BigDecimal("1")));
    }

    @Test
    public void testGetLengthMB() throws Exception {
        resource = new MockHippoResource(1024 * 1024 * 8);
        assertTrue("Expected 1 MB, but got: " + resource.getLengthMB(), resource.getLengthMB().equals(new BigDecimal("1")));
        resource = new MockHippoResource(1024*1024*8 * 10);
        assertTrue("Expected 10 MB, but got: " + resource.getLengthMB(), resource.getLengthMB().equals(new BigDecimal("10")));

    }


}

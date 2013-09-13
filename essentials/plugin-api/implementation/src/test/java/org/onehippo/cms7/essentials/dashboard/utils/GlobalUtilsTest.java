package org.onehippo.cms7.essentials.dashboard.utils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;


/**
 * @version "$Id: GlobalUtilsTest.java 174806 2013-08-23 09:22:46Z mmilicevic $"
 */
public class GlobalUtilsTest {

    private static Logger log = LoggerFactory.getLogger(GlobalUtilsTest.class);

    @Test
    public void testGenerateMethodName() throws Exception {
        String name = GlobalUtils.createMethodName(null);
        assertEquals(EssentialConst.INVALID_METHOD_NAME, name);
        name = GlobalUtils.createMethodName(":");
        assertEquals(EssentialConst.INVALID_METHOD_NAME, name);
        name = GlobalUtils.createMethodName("foo");
        assertEquals("getFoo", name);
        name = GlobalUtils.createMethodName("foo:");
        assertEquals("getFoo", name);
        name = GlobalUtils.createMethodName("foo:");
        assertEquals("getFoo", name);
        name = GlobalUtils.createMethodName("foo:bar");
        assertEquals("getBar", name);
        name = GlobalUtils.createMethodName("::::::");
        assertEquals(EssentialConst.INVALID_METHOD_NAME, name);
        name = GlobalUtils.createMethodName("foo bar id");
        assertEquals("getFoobarid", name);

    }


    @Test
    public void testGenerateClassName() throws Exception {
        String name = GlobalUtils.createClassName(null);
        assertEquals(EssentialConst.INVALID_CLASS_NAME, name);
        name = GlobalUtils.createClassName(":");
        assertEquals(EssentialConst.INVALID_CLASS_NAME, name);
        name = GlobalUtils.createClassName("foo:");
        assertEquals("Foo", name);
        name = GlobalUtils.createClassName("foo:bar");
        assertEquals("Bar", name);


    }



    @Test
    public void testReadTextFile() throws Exception {

    }

    @Test
    public void testWriteToFile() throws Exception {

    }
}

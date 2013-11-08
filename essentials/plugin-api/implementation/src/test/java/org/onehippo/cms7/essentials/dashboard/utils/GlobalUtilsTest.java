package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


/**
 * @version "$Id: GlobalUtilsTest.java 174806 2013-08-23 09:22:46Z mmilicevic $"
 */
public class GlobalUtilsTest {

    public static final String REPLACE_NAMESPACE = "testnamespace";
    public static final String REPLACE_DOC_NAME = "mytestname";


    @Test
    public void testReplacePlaceholders() throws Exception {
        final InputStream resourceAsStream = getClass().getResourceAsStream("/news_template.xml");
        final StringBuilder myBuilder = GlobalUtils.readStreamAsText(resourceAsStream);
        assertTrue("expected valid String", myBuilder != null);

        String input = myBuilder.toString();
        String output = input;
        assertEquals(input, output);
        output = GlobalUtils.replacePlaceholders(input, "NAMESPACE", REPLACE_NAMESPACE);
        assertNotEquals(input, output);
        input = output;
        assertEquals(input, output);
        output = GlobalUtils.replacePlaceholders(input, "DOCUMENT_NAME", REPLACE_DOC_NAME);
        assertNotEquals(input, output);
        assertTrue("expected NAMESPACE replacement", output.contains(REPLACE_NAMESPACE));
        assertTrue("expected DOCUMENT_NAME replacement", output.contains(REPLACE_DOC_NAME));


    }

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

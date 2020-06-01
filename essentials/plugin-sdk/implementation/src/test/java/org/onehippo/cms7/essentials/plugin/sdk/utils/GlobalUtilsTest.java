/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.io.InputStream;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @version "$Id: GlobalUtilsTest.java 174806 2013-08-23 09:22:46Z mmilicevic $"
 */
public class GlobalUtilsTest extends BaseTest {

    private static final String REPLACE_NAMESPACE = "testnamespace";
    private static final String REPLACE_DOC_NAME = "mytestname";
    private static final String CLASS_NAME_EXPECTED = "org-onehippo-cms7-essentials-plugin-sdk-utils-GlobalUtilsTest";


    @Test
    public void testValidFileName() throws Exception {
        final String myFileName = GlobalUtils.validFileName(getClass().getName());
        assertEquals(CLASS_NAME_EXPECTED, myFileName);
    }

    @Test
    public void testNewInstance() throws Exception {
        String myString = GlobalUtils.newInstance(String.class);
        assertTrue(myString != null);
        // test new instance from string:
        myString = GlobalUtils.newInstance(String.class.getName());
        assertTrue(myString != null);

        Log4jInterceptor.onError().deny(GlobalUtils.class).run(() -> {
            // not found exception
            assertNull(GlobalUtils.newInstance("com.foo.Bar.Baz"));
        });

        Log4jInterceptor.onError().deny(GlobalUtils.class).run(() -> {
            // cast exception
            assertNull(GlobalUtils.newInstance(Integer.class.getName()));
        });
    }

    @Test
    public void testReplacePlaceholders() throws Exception {
        final InputStream resourceAsStream = getClass().getResourceAsStream("/news_template.xml");
        String input = GlobalUtils.readStreamAsText(resourceAsStream);
        assertTrue("expected valid String", input != null);
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

/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import javax.servlet.ServletConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link org.hippoecm.hst.servlet.BinariesServlet}. Since the tested class is originally written by Hippo,
 * only the added logic is tested. The original logic is left untested.
 */
public class BinariesServletTest {

    private BinariesServlet binariesServlet;

    /**
     * @see Before
     */
    @Before
    public void setUp() {
        binariesServlet = new BinariesServlet();
    }

    /**
     * @see After
     */
    @After
    public void tearDown() {
        binariesServlet.destroy();
    }

    /**
     * Precondition: All possible init-params are set.
     *
     * Pass condition: Content types init-param is properly parsed as an array of Strings and the filename property is
     *                 properly retrieved from init-params.
     */
    @Test
    public void testInit() throws Exception {
        ServletConfig servletConfig = createMock(ServletConfig.class);

        String mimeTypesString = "\n" + "                application/pdf\n\r" + "                application/rtf\n"
                + "                application/excel\r\n" + "            ";
        String expectedFilenameProperty = "myschema:filename";
        String expectedBasePath = "/path/to/binaries";

        expect(servletConfig.getInitParameter("contentDispositionContentTypes")).andReturn(mimeTypesString);
        expect(servletConfig.getInitParameter("contentDispositionFilenameProperty"))
                .andReturn(expectedFilenameProperty);
        expect(servletConfig.getInitParameter("baseBinariesContentPath")).andReturn(expectedBasePath);
        expect(servletConfig.getInitParameter("binaryResourceNodeType")).andReturn("hippo:resource");
        expect(servletConfig.getInitParameter("binaryDataPropName")).andReturn("my:data");
        expect(servletConfig.getInitParameter("binaryMimeTypePropName")).andReturn("my:type");
        expect(servletConfig.getInitParameter("binaryLastModifiedPropName")).andReturn("my:lastmod");
        expect(servletConfig.getInitParameter("contentDispositionFilenameEncoding")).andReturn("user-agent-specific");
        expect(servletConfig.getInitParameter("forceContentDispositionRequestParamName")).andReturn("download");
        expect(servletConfig.getInitParameter("set-expires-headers")).andReturn("false");
        expect(servletConfig.getInitParameter("set-content-length-header")).andReturn("false");

        replay(servletConfig);
        binariesServlet.init(servletConfig);
        verify(servletConfig);
    }

    /**
     * Precondition: None of the possible init-params are set.
     *
     * Pass condition: No exception is thrown and the configuration fields have their default values set
     */
    @Test
    public void testInit_noParams() throws Exception {
        ServletConfig servletConfig = createMock(ServletConfig.class);

        expect(servletConfig.getInitParameter("contentDispositionContentTypes")).andReturn(null);
        expect(servletConfig.getInitParameter("contentDispositionFilenameProperty")).andReturn(null);
        expect(servletConfig.getInitParameter("baseBinariesContentPath")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryResourceNodeType")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryDataPropName")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryMimeTypePropName")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryLastModifiedPropName")).andReturn(null);
        expect(servletConfig.getInitParameter("contentDispositionFilenameEncoding")).andReturn(null);

        expect(servletConfig.getInitParameter("set-expires-headers")).andReturn(null);
        expect(servletConfig.getInitParameter("set-content-length-header")).andReturn(null);
        expect(servletConfig.getInitParameter("forceContentDispositionRequestParamName")).andReturn(null);
        replay(servletConfig);
        binariesServlet.init(servletConfig);
        verify(servletConfig);
    }

}

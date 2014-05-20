/*
 *  Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InitializationProcessorImplTest
 */
public class InitializationProcessorImplTest {

    private static Logger log = LoggerFactory.getLogger(InitializationProcessorImplTest.class);

    private InitializationProcessorImpl processor;

    @Before
    public void before() {
        processor = new InitializationProcessorImpl();
    }

    /*
     * REPO-969: It works fine when the file: URL is on non-Windows system,
     *           but it throws "IllegalArgumentException: URI is not hierarchical"
     *           if the file: URL denotes a Windows URL like 'file:C:/a/b/c/...'.
     */
    @Test
    public void testGetBaseZipFileFromURL() throws Exception {
        URL url = new URL("file:/a/b/c.jar!/d/e/f.xml");
        assertEquals("/a/b/c.jar!/d/e/f.xml", url.getFile());
        File baseFile = processor.getBaseZipFileFromURL(url);
        assertTrue(baseFile.getPath().endsWith("c.jar"));

        url = new URL("file:C:/a/b/c.jar!/d/e/f.xml");
        assertEquals("C:/a/b/c.jar!/d/e/f.xml", url.getFile());
        baseFile = processor.getBaseZipFileFromURL(url);
        assertTrue(baseFile.getPath().endsWith("c.jar"));
    }

}

/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository;

import java.util.Collections;
import java.util.Locale;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.Localized;

import org.junit.Ignore;
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalizedTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private String[] content = new String[] {
        "/test",                                   "nt:unstructured",
        "/test/folder",                            "hippostd:folder",
        "jcr:mixinTypes",                          "hippo:harddocument",
        "jcr:mixinTypes",                          "hippo:translated",
        "/test/folder/hippo:translation",          "hippo:translation",
        "hippo:language",                          "en",
        "hippo:message",                           "foo",
        "/test/folder/hippo:translation",          "hippo:translation",
        "hippo:language",                          "nl",
        "hippo:message",                           "aap",
        "/test/folder/document",                   "hippo:handle",
        "jcr:mixinTypes",                          "hippo:hardhandle",
        "jcr:mixinTypes",                          "hippo:translated",
        "/test/folder/document/hippo:translation", "hippo:translation",
        "hippo:language",                          "en",
        "hippo:message",                           "bar",
        "/test/folder/document/hippo:translation", "hippo:translation",
        "hippo:language",                          "nl",
        "hippo:message",                           "noot",
        "/test/folder/document/document",          "hippo:document",
        "jcr:mixinTypes",                          "hippo:harddocument"
    };
    protected final Localized englishLocalized = Localized.getInstance(Locale.ENGLISH);;
    protected final Localized dutchLocalized = Localized.getInstance(Collections.singletonMap("hippostd:language", Collections.singletonList("nl")));

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(session, content);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }


    @Ignore
    public void testFolderTranslations() throws RepositoryException {
        HippoNode folder = (HippoNode) session.getRootNode().getNode("test/folder");
        assertEquals("foo", folder.getLocalizedName());
        assertEquals("foo", folder.getLocalizedName(englishLocalized));
        assertEquals("aap", folder.getLocalizedName(dutchLocalized));
    }

    @Test
    public void testDocumentTranslations() throws RepositoryException {
        HippoNode document = (HippoNode) session.getRootNode().getNode("test/folder/document");
        assertEquals("bar", document.getLocalizedName());
        assertEquals("bar", document.getLocalizedName(englishLocalized));
        assertEquals("noot", document.getLocalizedName(dutchLocalized));
        document = (HippoNode) session.getRootNode().getNode("test/folder/document/document");
        assertEquals("bar", document.getLocalizedName());
        assertEquals("bar", document.getLocalizedName(englishLocalized));
        assertEquals("noot", document.getLocalizedName(dutchLocalized));
    }
}

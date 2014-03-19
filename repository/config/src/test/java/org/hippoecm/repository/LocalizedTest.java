/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;

public class LocalizedTest extends RepositoryTestCase {

    private String[] content = new String[] {
        "/test",                                   "nt:unstructured",
        "/test/folder",                            "hippostd:folder",
        "jcr:mixinTypes",                          "mix:versionable",
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
        "jcr:mixinTypes",                          "mix:versionable",
        "/test/xx0",                               "hippostd:folder",
        "jcr:mixinTypes",                          "mix:versionable",
        "jcr:mixinTypes",                          "hippo:translated",
        "/test/xx0/hippo:translation",             "hippo:translation",
        "hippo:language",                          "en",
        "hippo:message",                           "en0",
        "/test/xx0/hippo:translation",             "hippo:translation",
        "hippo:language",                          "nl",
        "hippo:message",                           "nl0",
        "/test/xx0/hippo:translation",             "hippo:translation",
        "hippo:language",                          "de",
        "hippo:message",                           "de0",

        "/test/xx1",                               "hippostd:folder",
        "jcr:mixinTypes",                          "mix:versionable",
        "jcr:mixinTypes",                          "hippo:translated",
        "/test/xx1/hippo:translation",             "hippo:translation",
        "hippo:message",                           "en1",
        "/test/xx1/hippo:translation",             "hippo:translation",
        "hippo:language",                          "nl",
        "hippo:message",                           "nl1",
        "/test/xx1/hippo:translation",             "hippo:translation",
        "hippo:language",                          "de",
        "hippo:message",                           "de1",

        "/test/xx2",                               "hippostd:folder",
        "jcr:mixinTypes",                          "mix:versionable",
        "jcr:mixinTypes",                          "hippo:translated",
        "/test/xx2/hippo:translation",             "hippo:translation",
        "hippo:language",                          "en",
        "hippo:message",                           "en2",
        "/test/xx2/hippo:translation",             "hippo:translation",
        "hippo:message",                           "nl2",
        "/test/xx2/hippo:translation",             "hippo:translation",
        "hippo:language",                          "de",
        "hippo:message",                           "de2",
        "/test/xx3",                               "hippostd:folder",
        "jcr:mixinTypes",                          "mix:versionable",
        "jcr:mixinTypes",                          "hippo:translated",
        "/test/xx3/hippo:translation",             "hippo:translation",
        "hippo:language",                          "en",
        "hippo:message",                           "en3",
        "/test/xx3/hippo:translation",             "hippo:translation",
        "hippo:language",                          "nl",
        "hippo:message",                           "nl3",
        "/test/xx3/hippo:translation",             "hippo:translation",
        "hippo:message",                           "de3"
    };
    protected final Localized englishLocalized = Localized.getInstance(Locale.ENGLISH);
    protected final Localized dutchLocalized = Localized.getInstance(Collections.singletonMap("hippostd:language",
                                                                                              Collections.singletonList("nl")));
    protected final Localized germanLocalized = Localized.getInstance(Locale.GERMAN);
    protected final Localized frenchLocalized = Localized.getInstance(Locale.FRENCH);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(content, session);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }


    @Test
    public void testFolderTranslations() throws RepositoryException {
        HippoNode folder = (HippoNode) session.getRootNode().getNode("test/folder");
        assertEquals("foo", folder.getLocalizedName());
        assertEquals("foo", folder.getLocalizedName(englishLocalized));
        assertEquals("aap", folder.getLocalizedName(dutchLocalized));
    }

    @Test
    public void testFolderTranslationsGenericTranslation() throws RepositoryException {
        HippoNode folder = (HippoNode) session.getRootNode().getNode("test/xx0");
        assertEquals("en0", folder.getLocalizedName());
        assertEquals("en0", folder.getLocalizedName(englishLocalized));
        assertEquals("nl0", folder.getLocalizedName(dutchLocalized));
        assertEquals("de0", folder.getLocalizedName(germanLocalized));
        assertEquals("xx0", folder.getLocalizedName(frenchLocalized));

        folder = (HippoNode) session.getRootNode().getNode("test/xx1");
        assertEquals("en1", folder.getLocalizedName());
        assertEquals("en1", folder.getLocalizedName(englishLocalized));
        assertEquals("nl1", folder.getLocalizedName(dutchLocalized));
        assertEquals("de1", folder.getLocalizedName(germanLocalized));
        assertEquals("en1", folder.getLocalizedName(frenchLocalized));

        folder = (HippoNode) session.getRootNode().getNode("test/xx2");
        assertEquals("nl2", folder.getLocalizedName());
        assertEquals("en2", folder.getLocalizedName(englishLocalized));
        assertEquals("nl2", folder.getLocalizedName(dutchLocalized));
        assertEquals("de2", folder.getLocalizedName(germanLocalized));
        assertEquals("nl2", folder.getLocalizedName(frenchLocalized));

        folder = (HippoNode) session.getRootNode().getNode("test/xx3");
        assertEquals("de3", folder.getLocalizedName());
        assertEquals("en3", folder.getLocalizedName(englishLocalized));
        assertEquals("nl3", folder.getLocalizedName(dutchLocalized));
        assertEquals("de3", folder.getLocalizedName(germanLocalized));
        assertEquals("de3", folder.getLocalizedName(frenchLocalized));
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

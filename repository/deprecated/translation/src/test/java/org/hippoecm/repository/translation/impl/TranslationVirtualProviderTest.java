/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.translation.impl;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.util.Utilities.dump;
import static org.junit.Assert.assertTrue;

public class TranslationVirtualProviderTest extends RepositoryTestCase {

    static final String FOLDER_T9N_ID = "700a09f1-eac5-482c-a09e-ec0a6a5d6abc";

    String[] content = {
            "/test/folder", "hippostd:folder",
                "jcr:mixinTypes", "mix:versionable," + HippoTranslationNodeType.NT_TRANSLATED,
                HippoTranslationNodeType.ID, FOLDER_T9N_ID,
                HippoTranslationNodeType.LOCALE, "en",
                "/test/folder/" + HippoTranslationNodeType.TRANSLATIONS, HippoTranslationNodeType.NT_TRANSLATIONS,
                "/test/folder/document", "hippo:handle",
                    "jcr:mixinTypes", "hippo:hardhandle",
                    "/test/folder/document/document", "hippo:testdocument",
                        "hippostd:state", "unpublished",
                        "hippostd:holder", "admin",
            "/test/folder_nl", "hippostd:folder",
                "jcr:mixinTypes", "mix:versionable," + HippoTranslationNodeType.NT_TRANSLATED,
                HippoTranslationNodeType.ID, FOLDER_T9N_ID,
                HippoTranslationNodeType.LOCALE, "nl",
                "/test/folder_nl/" + HippoTranslationNodeType.TRANSLATIONS, HippoTranslationNodeType.NT_TRANSLATIONS,
    };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode("test");
        session.save();

        build(content, session);
        session.save();
        session.refresh(false);
    }

    @Test
    public void testVirtualSubTreeIsPopulatedForFolders() throws RepositoryException {
        dump(session.getNode("/test/folder"));
        session.getNode("/test/folder/" + HippoTranslationNodeType.TRANSLATIONS);
        assertTrue(session.nodeExists("/test/folder/" + HippoTranslationNodeType.TRANSLATIONS + "/nl"));
    }

}
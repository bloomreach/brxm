/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.hippodocumentresource;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.model.HippoDocumentRepresentation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommonFolderContentHippoDocumentResourceTest extends AbstractHippoDocumentResourceTest {


    private String getCommonFolderRequestConfigIdentifier() throws RepositoryException {
        return session.getNode("/unittestcontent/documents/unittestproject/common").getIdentifier();
    }

    @Test
    public void assert_homepage_representation_has_pathInfo_home_and_not_empty() throws Exception {

        // request for the homepage and set the homepage as REQUEST_CONFIG_NODE_IDENTIFIER hence 'true'
        // homepage has pathInfo = ""
        HippoDocumentRepresentation representation = createRootContentRepresentation("", getCommonFolderRequestConfigIdentifier());
        assertEquals(2, representation.getItems().size());

        final HippoDocumentRepresentation homePageRepresentation = representation.getItems().get(0);
        assertEquals("Home Page", homePageRepresentation.getDisplayName());
        assertEquals("home", homePageRepresentation.getPathInfo());

        System.out.println(representation);
    }
}

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

package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.treepickerrepresentation;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.HippoDocumentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.AbstractMountResourceTest;
import org.hippoecm.hst.site.HstServices;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AbstractHippoDocumentResourceTest extends AbstractPageComposerTest {

    protected TreePickerRepresentation createRootContentRepresentation(final String pathInfo, final String requestConfigContentIdentifier) throws Exception {
        mockNewRequest(session, "localhost", pathInfo, requestConfigContentIdentifier);
        final HippoDocumentResource resource = createResource();
        final Response response = resource.get("");
        final ExtResponseRepresentation representation = (ExtResponseRepresentation) response.getEntity();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        return (TreePickerRepresentation) representation.getData();
    }

    protected TreePickerRepresentation createExpandedTreeRepresentation(final String pathInfo,
                                                                        final String requestConfigContentIdentifier,
                                                                        final String siteMapPathInfo) throws Exception {
        mockNewRequest(session, "localhost", pathInfo, requestConfigContentIdentifier);
        final HippoDocumentResource resource = createResource();
        final Response response = resource.get(siteMapPathInfo);
        final ExtResponseRepresentation representation = (ExtResponseRepresentation) response.getEntity();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        return (TreePickerRepresentation) representation.getData();
    }

    protected void mockNewRequest(Session jcrSession, String host, String pathInfo, final String requestConfigContentIdentifier) throws Exception {
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(host, pathInfo);
         ((HstMutableRequestContext) ctx).setSession(jcrSession);
        ((HstMutableRequestContext) ctx).setLinkCreator(HstServices.getComponentManager().getComponent(HstLinkCreator.class.getName()));
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, requestConfigContentIdentifier);
    }

    protected HippoDocumentResource createResource() {
        MountResource mountResource = AbstractMountResourceTest.createResource();
        final HippoDocumentResource hippoDocumentResource = new HippoDocumentResource();
        hippoDocumentResource.setPageComposerContextService(mountResource.getPageComposerContextService());
        return hippoDocumentResource;
    }

    protected String getRootContentRequestConfigIdentifier() throws RepositoryException {
        return session.getNode("/unittestcontent/documents/unittestproject").getIdentifier();
    }

    protected String getCommonFolderRequestConfigIdentifier() throws RepositoryException {
        return session.getNode("/unittestcontent/documents/unittestproject/common").getIdentifier();
    }


    protected void rootContentRepresentationAssertions(final TreePickerRepresentation representation) {
        assertEquals("unittestproject",representation.getNodeName());
        assertFalse("The root content folder is not selectable", representation.isSelectable());
        assertNull(representation.getPathInfo());
        assertFalse("The root content folder is never selected", representation.isSelected());
        assertFalse("The root content folder does not have documents", representation.isContainsDocuments());
        assertTrue("The root content folder does have folders", representation.isContainsFolders());

        for (TreePickerRepresentation child : representation.getItems()) {
            assertEquals("Children should *not* be populated as they should be lazily loaded",
                    0, child.getItems().size());
        }
    }


}

/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.HippoDocumentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapItemResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.PagesHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AbstractTestTreePickerRepresentation extends AbstractPageComposerTest {

    protected AbstractTreePickerRepresentation createRootContentRepresentation(final String pathInfo, final String requestConfigContentIdentifier) throws Exception {
        mockNewRequest(session, "localhost", pathInfo, requestConfigContentIdentifier);
        final HippoDocumentResource resource = createHippoDocumentResource();
        final Response response = resource.get("");
        final ResponseRepresentation representation = (ResponseRepresentation) response.getEntity();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        return (AbstractTreePickerRepresentation) representation.getData();
    }

    protected AbstractTreePickerRepresentation createExpandedTreeContentRepresentation(final String pathInfo,
                                                                               final String requestConfigContentIdentifier,
                                                                               final String siteMapPathInfo) throws Exception {
        mockNewRequest(session, "localhost", pathInfo, requestConfigContentIdentifier);
        final HippoDocumentResource resource = createHippoDocumentResource();
        final Response response = resource.get(siteMapPathInfo);
        final ResponseRepresentation representation = (ResponseRepresentation) response.getEntity();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        return (AbstractTreePickerRepresentation) representation.getData();
    }

    protected AbstractTreePickerRepresentation createSiteMapRepresentation(final String pathInfo,
                                                                   final String requestConfigContentIdentifier) throws Exception {
        mockNewRequest(session, "localhost", pathInfo, requestConfigContentIdentifier);
        final SiteMapResource siteMapResource = createSiteMapResource();
        final Response response = siteMapResource.getSiteMapTreePicker();
        final ResponseRepresentation representation = (ResponseRepresentation) response.getEntity();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        return (AbstractTreePickerRepresentation) representation.getData();
    }

    protected AbstractTreePickerRepresentation createSiteMapItemRepresentation(final String pathInfo,
                                                                       final String requestConfigContentIdentifier) throws Exception {
        mockNewRequest(session, "localhost", pathInfo, requestConfigContentIdentifier);
        final SiteMapItemResource siteMapItemResource = createSiteMapItemResource();
        final Response response = siteMapItemResource.getSiteMapItemTreePicker();
        final ResponseRepresentation representation = (ResponseRepresentation) response.getEntity();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        return (AbstractTreePickerRepresentation) representation.getData();
    }

    protected AbstractTreePickerRepresentation createExpandedSiteMapRepresentation(final String pathInfo,
                                                                       final String requestConfigContentIdentifier,
                                                                       final String siteMapPathInfo) throws Exception {
        mockNewRequest(session, "localhost", pathInfo, requestConfigContentIdentifier);
        final SiteMapResource siteMapResource = createSiteMapResource();
        final Response response = siteMapResource.getSiteMapTreePicker(siteMapPathInfo);
        final ResponseRepresentation representation = (ResponseRepresentation) response.getEntity();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        return (AbstractTreePickerRepresentation) representation.getData();
    }

    protected void mockNewRequest(Session jcrSession, String host, String pathInfo, final String requestConfigContentIdentifier) throws Exception {
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(host, pathInfo);
         ((HstMutableRequestContext) ctx).setSession(jcrSession);
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, requestConfigContentIdentifier);
    }

    protected HippoDocumentResource createHippoDocumentResource() {
        final HippoDocumentResource hippoDocumentResource = new HippoDocumentResource();
        hippoDocumentResource.setPageComposerContextService(mountResource.getPageComposerContextService());
        return hippoDocumentResource;
    }

    protected SiteMapResource createSiteMapResource() {

        final PagesHelper pagesHelper = new PagesHelper();
        pagesHelper.setPageComposerContextService(mountResource.getPageComposerContextService());
        final SiteMapHelper siteMapHelper = new SiteMapHelper();
        siteMapHelper.setPageComposerContextService(mountResource.getPageComposerContextService());
        siteMapHelper.setPagesHelper(pagesHelper);

        final SiteMapResource siteMapResource = new SiteMapResource();
        siteMapResource.setPageComposerContextService(mountResource.getPageComposerContextService());
        siteMapResource.setSiteMapHelper(siteMapHelper);
        siteMapResource.setValidatorFactory(new ValidatorFactory());
        return siteMapResource;
    }

    protected SiteMapItemResource createSiteMapItemResource() {

        final PagesHelper pagesHelper = new PagesHelper();
        pagesHelper.setPageComposerContextService(mountResource.getPageComposerContextService());
        final SiteMapHelper siteMapHelper = new SiteMapHelper();
        siteMapHelper.setPageComposerContextService(mountResource.getPageComposerContextService());
        siteMapHelper.setPagesHelper(pagesHelper);

        final SiteMapItemResource siteMapItemResource = new SiteMapItemResource();
        siteMapItemResource.setPageComposerContextService(mountResource.getPageComposerContextService());
        siteMapItemResource.setSiteMapHelper(siteMapHelper);
        return siteMapItemResource;
    }

    protected String getRootContentConfigIdentifier() throws RepositoryException {
        return session.getNode("/unittestcontent/documents/unittestproject").getIdentifier();
    }

    protected String getCommonFolderConfigIdentifier() throws RepositoryException {
        return session.getNode("/unittestcontent/documents/unittestproject/common").getIdentifier();
    }

    protected String getSiteMapIdentifier() throws RepositoryException {
        return session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").getIdentifier();
    }


    protected String getSiteMapItemIdentifier(final String pathInfo) throws RepositoryException {
        return session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/" + pathInfo).getIdentifier();
    }

    protected void rootContentRepresentationAssertions(final AbstractTreePickerRepresentation representation) {

        assertEquals(representation.getPickerType(), "documents");
        assertEquals("unittestproject", representation.getNodeName());
        assertFalse("The root content folder is not selectable", representation.isSelectable());
        assertNull(representation.getPathInfo());
        assertFalse("The root content folder is never selected", representation.isSelected());
        assertTrue("The root content folder does have folders", representation.isExpandable());
        assertFalse("Root should always be expanded.",representation.isCollapsed());
        for (AbstractTreePickerRepresentation child : representation.getItems()) {
            assertEquals(child.getPickerType(), "documents");
            assertEquals("Children should *not* be populated as they should be lazily loaded",
                    0, child.getItems().size());
        }
    }


}

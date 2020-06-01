/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import javax.jcr.Node;

import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompositeHstLinkCreatorIT extends AbstractHstLinkRewritingIT {

    @Test
    public void create_link() throws Exception {

        final HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        final Node node = requestContext.getSession().getNode("/extracontent/documents/extraproject/News/News1");

        final HstLink hstLink = linkCreator.create(node, requestContext);
        assertEquals("http://localhost/site2/extra/news/News1.html", hstLink.toUrlForm(requestContext, true));

        final HstLink hstLinkById = linkCreator.create(node.getIdentifier(), requestContext.getSession(), requestContext);
        assertEquals("http://localhost/site2/extra/news/News1.html", hstLinkById.toUrlForm(requestContext, true));

        final HstLink notFoundLink = linkCreator.create(node, requestContext.getResolvedMount().getMount());
        assertTrue(notFoundLink.isNotFound());
    }

    @Test
    public void create_canonical_link() throws Exception {
        final HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/news");
        final Node node = requestContext.getSession().getNode("/extracontent/documents/extraproject/News/News1");

        HstLink canonicalNewsLink = linkCreator.createCanonical(node, requestContext);
        assertEquals("wrong canonical link.getPath for News/News1", "news/News1.html", canonicalNewsLink.getPath());

        canonicalNewsLink = linkCreator.create(node, requestContext.getResolvedMount().getMount());
        assertTrue(canonicalNewsLink.isNotFound());

        canonicalNewsLink = linkCreator.create(node, requestContext.getResolvedMount().getMount(), true);
        assertFalse(canonicalNewsLink.isNotFound());

    }
}

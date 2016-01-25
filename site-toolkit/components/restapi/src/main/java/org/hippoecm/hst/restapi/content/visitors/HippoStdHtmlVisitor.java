/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.restapi.content.visitors;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.hst.restapi.content.html.RestApiHtmlParser;
import org.hippoecm.hst.restapi.content.html.ParsedContent;
import org.hippoecm.hst.restapi.content.linking.Link;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_CONTENT;
import static org.hippoecm.repository.HippoStdNodeType.NT_HTML;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETSELECT;
import static org.hippoecm.repository.api.HippoNodeType.NT_MIRROR;

public class HippoStdHtmlVisitor extends DefaultNodeVisitor {

    private static final Logger log = LoggerFactory.getLogger(HippoStdHtmlVisitor.class);

    private RestApiHtmlParser restApiHtmlParser;

    public void setRestApiHtmlParser(RestApiHtmlParser restApiHtmlParser) {
        this.restApiHtmlParser = restApiHtmlParser;
    }

    @Override
    public String getNodeType() {
        return NT_HTML;
    }

    @Override
    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response) throws RepositoryException {
        super.visitNode(context, node, response);

        final ParsedContent parsedContent = restApiHtmlParser.parseContent(context, node);
        if (parsedContent == null) {
            return;
        }
        response.put("content", parsedContent.getRewrittenHtml());
        final Map<String, Link> links = parsedContent.getLinks();
        if (!links.isEmpty()) {
            response.put("links", links);
        }
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                   final Property property) throws RepositoryException {
        if (HIPPOSTD_CONTENT.equals(property.getName())) {
            return true;
        }
        return super.skipProperty(context, propertyType, property);
    }

    protected boolean skipChild(final ResourceContext context, final ContentTypeChild childType,
                                final Node child) throws RepositoryException {
        if (child.isNodeType(NT_MIRROR) || child.isNodeType(NT_FACETSELECT)) {
            return true;
        } else {
            return super.skipChild(context, childType, child);
        }
    }
}
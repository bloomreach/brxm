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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.hst.restapi.content.linking.Link;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_FILENAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_TEXT;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCE;
import static org.onehippo.repository.util.JcrConstants.JCR_DATA;
import static org.onehippo.repository.util.JcrConstants.JCR_ENCODING;
import static org.onehippo.repository.util.JcrConstants.JCR_LAST_MODIFIED;
import static org.onehippo.repository.util.JcrConstants.JCR_MIME_TYPE;

public class HippoResourceVisitor extends DefaultNodeVisitor {

    private static final List<String> skipProperties = new ArrayList<>(Arrays.asList(
            JCR_ENCODING,
            JCR_DATA,
            JCR_MIME_TYPE,
            JCR_LAST_MODIFIED,
            HIPPO_TEXT,
            HIPPO_FILENAME
    ));

    /**
     * If a resource contains MIME type application/vnd.hippo.blank it is marked as blank and contains no usable data.
     */
    protected static final String MIME_TYPE_HIPPO_BLANK = "application/vnd.hippo.blank";

    @Override
    public String getNodeType() {
        return NT_RESOURCE;
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);
        response.put("mimeType", node.getProperty(JCR_MIME_TYPE).getString());
        response.put("lastModified", node.getProperty(JCR_LAST_MODIFIED).getString());
        response.put("isBlank", MIME_TYPE_HIPPO_BLANK.equals(response.get("mimeType")));
        String filename = null;
        if (node.hasProperty(HIPPO_FILENAME)) {
            filename = node.getProperty(HIPPO_FILENAME).getString();
            response.put("filename", filename);
        }
        long length = node.getProperty(JCR_DATA).getLength();
        response.put("length", length);

        final HstLink hstLink = context.getRequestContext().getHstLinkCreator().create(node, context.getRequestContext());
        Link link = context.getRestApiLinkCreator().convert(context, node.getIdentifier(), hstLink);
        response.put("link",link);
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                   final Property property) throws RepositoryException {
        if (skipProperties.contains(property.getName())) {
            return true;
        }
        return super.skipProperty(context, propertyType, property);
    }
}

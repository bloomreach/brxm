/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.hst.restapi.content.linking.Link;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DOCBASE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_FACETS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MODES;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VALUES;

abstract class AbstractLinkVisitor extends DefaultNodeVisitor {

    private static final Logger log = LoggerFactory.getLogger(AbstractLinkVisitor.class);

    private static final List<String> skipProperties = new ArrayList<>(Arrays.asList(
            HIPPO_DOCBASE,
            HIPPO_FACETS,
            HIPPO_MODES,
            HIPPO_VALUES
    ));

    @Override
    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response) throws RepositoryException {
        super.visitNode(context, node, response);
        final String docbase = node.getProperty(HIPPO_DOCBASE).getString();
        if (StringUtils.isBlank(docbase) || docbase.equals("cafebabe-cafe-babe-cafe-babecafebabe")) {
            response.put("link", Link.invalid);
            return;
        }
        else {
            final HstRequestContext requestContext = context.getRequestContext();
            final HstLink hstLink = requestContext.getHstLinkCreator().create(docbase, requestContext.getSession(), requestContext);
            final Link link = context.getRestApiLinkCreator().convert(context, docbase, hstLink);
            response.put("link", link);
        }
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                   final Property property) throws RepositoryException {
        if (skipProperties.contains(property.getName())) {
            return true;
        }
        return super.skipProperty(context, propertyType, property);
    }
}
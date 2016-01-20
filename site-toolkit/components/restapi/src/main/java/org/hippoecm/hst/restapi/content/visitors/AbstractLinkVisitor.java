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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.restapi.content.ResourceContext;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DOCBASE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_FACETS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MODES;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VALUES;

abstract class AbstractLinkVisitor extends DefaultNodeVisitor {

    private static final List<String> skipProperties = new ArrayList<>(Arrays.asList(
            HIPPO_DOCBASE,
            HIPPO_FACETS,
            HIPPO_MODES,
            HIPPO_VALUES
    ));

    @Override
    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response) throws RepositoryException {
        super.visitNode(context, node, response);
        try {
            final String docbase = node.getProperty(HIPPO_DOCBASE).getString();
            if (StringUtils.isBlank(docbase) || docbase.equals("cafebabe-cafe-babe-cafe-babecafebabe")) {
                // noop
            }
            else {
                response.put("id",docbase);
                // TODO link rewriting - use generic HST methods to construct URL
                response.put("url","http://localhost:8080/site/api/documents/" + docbase);
            }
        } catch (RepositoryException e) {
            // log warning
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
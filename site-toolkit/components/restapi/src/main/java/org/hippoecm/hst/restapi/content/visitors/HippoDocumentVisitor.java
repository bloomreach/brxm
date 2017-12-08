/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;

public class HippoDocumentVisitor extends DefaultNodeVisitor {

    @Override
    public String getNodeType() {
        return NT_DOCUMENT;
    }

    @Override
    public void visit(final ResourceContext context, final Node node, final Map<String, Object> response) throws RepositoryException {
        visitNode(context, node, response);
        visitNodeItems(context, node, response);
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);
        visitLocaleProperty(context, node, response);
    }

    protected void visitLocaleProperty(final ResourceContext context, final Node node, Map<String, Object> response)
            throws RepositoryException {
        if (node.hasProperty(HippoTranslationNodeType.LOCALE)) {
            response.put("locale", node.getProperty(HippoTranslationNodeType.LOCALE).getString());
        }
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                   final Property property) throws RepositoryException {
        if (HIPPO_AVAILABILITY.equals(property.getName())) {
            return true;
        }
        return super.skipProperty(context, propertyType, property);
    }

    protected boolean skipChild(final ResourceContext context, final ContentTypeChild childType,
                                final Node child) throws RepositoryException {

        if (context.getIncludedAttributes().size() == 0 && !context.includeDocumentDataByDefault()) {
            return true;
        }
        if (context.getIncludedAttributes().size() > 0 && !context.getIncludedAttributes().contains(child.getName())) {
            return true;
        }
        return super.skipChild(context, childType, child);
    }

}

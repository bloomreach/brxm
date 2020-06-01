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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.restapi.ResourceContext;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE;

public class HippoPublicationWorkflowDocumentVisitor extends HippoPublishableDocumentVisitor {

    private static final List<String> skipProperties = new ArrayList<>(Arrays.asList(
            HIPPOSTDPUBWF_CREATION_DATE,
            HIPPOSTDPUBWF_CREATED_BY,
            HIPPOSTDPUBWF_LAST_MODIFIED_DATE,
            HIPPOSTDPUBWF_LAST_MODIFIED_BY,
            HIPPOSTDPUBWF_PUBLICATION_DATE
    ));

    @Override
    public String getNodeType() {
        return HIPPOSTDPUBWF_DOCUMENT;
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);
        addPropertyConditionally(context, "pubwfCreationDate", node.getProperty(HIPPOSTDPUBWF_CREATION_DATE).getString(), response);
        addPropertyConditionally(context, "pubwfLastModificationDate", node.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE).getString(), response);
        addPropertyConditionally(context, "pubwfCreationDate", node.getProperty(HIPPOSTDPUBWF_CREATION_DATE).getString(), response);
        if (node.hasProperty(HIPPOSTDPUBWF_PUBLICATION_DATE)) {
            addPropertyConditionally(context, "pubwfPublicationDate", node.getProperty(HIPPOSTDPUBWF_PUBLICATION_DATE).getString(), response);
        }
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                   final Property property) throws RepositoryException {
        if (skipProperties.contains(property.getName())) {
            return true;
        }
        if (context.getIncludedAttributes().size() == 0 && !context.includeDocumentDataByDefault()) {
            return true;
        }
        if (context.getIncludedAttributes().size() > 0 && !context.getIncludedAttributes().contains(property.getName())) {
            return true;
        }
        return super.skipProperty(context, propertyType, property);
    }
}

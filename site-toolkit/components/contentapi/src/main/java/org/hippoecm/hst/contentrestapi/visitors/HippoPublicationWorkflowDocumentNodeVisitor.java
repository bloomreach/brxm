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
package org.hippoecm.hst.contentrestapi.visitors;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE;

public class HippoPublicationWorkflowDocumentNodeVisitor extends HippoPublishableDocumentNodeVisitor {

    public HippoPublicationWorkflowDocumentNodeVisitor(final VisitorFactory visitorFactory) {
        super(visitorFactory);
    }

    @Override
    public String getNodeType() {
        return HIPPOSTDPUBWF_DOCUMENT;
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);
        response.put("pubwfCreationDate", node.getProperty(HIPPOSTDPUBWF_CREATION_DATE).getString());
        response.put("pubwfLastModificationDate", node.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE).getString());
        if (node.hasProperty(HIPPOSTDPUBWF_PUBLICATION_DATE)) {
            response.put("pubwfPublicationDate", node.getProperty(HIPPOSTDPUBWF_PUBLICATION_DATE).getString());
        }
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                   final Property property) throws RepositoryException {
        switch (property.getName()) {
            case HIPPOSTDPUBWF_CREATION_DATE:
            case HIPPOSTDPUBWF_LAST_MODIFIED_DATE:
            case HIPPOSTDPUBWF_PUBLICATION_DATE:
                return true;
            default:
                return super.skipProperty(context, propertyType, property);
        }
    }
}

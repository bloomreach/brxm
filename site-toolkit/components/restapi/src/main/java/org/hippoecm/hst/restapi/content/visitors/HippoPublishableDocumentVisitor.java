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

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_HOLDER;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.NT_PUBLISHABLE;

public class HippoPublishableDocumentVisitor extends HippoDocumentVisitor {

    private static final List<String> skipProperties = new ArrayList<>(Arrays.asList(
            HIPPOSTD_STATE,
            HIPPOSTD_HOLDER
    ));

    @Override
    public String getNodeType() {
        return NT_PUBLISHABLE;
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);
        addPropertyConditionally(context, "pubState", node.getProperty(HIPPOSTD_STATE).getString(), response);
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                   final Property property) throws RepositoryException {
        if (skipProperties.contains(property.getName())) {
            return true;
        }
        return super.skipProperty(context, propertyType, property);
    }

    protected void addPropertyConditionally(final ResourceContext context, final String name, final String value,
                                            final Map<String, Object> response ) {
        if(context.getIncludedAttributes().size() == 0 || context.getIncludedAttributes().contains(name)) {
            response.put(name, value);
        }
    }
}

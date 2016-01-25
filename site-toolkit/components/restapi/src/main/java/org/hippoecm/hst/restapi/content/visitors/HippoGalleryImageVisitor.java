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

import org.hippoecm.hst.restapi.ResourceContext;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

public class HippoGalleryImageVisitor extends HippoResourceVisitor {

    protected static final String NT_IMAGE = "hippogallery:image";
    protected static final String IMAGE_WIDTH = "hippogallery:width";
    protected static final String IMAGE_HEIGHT = "hippogallery:height";

    private static final List<String> skipProperties = new ArrayList<>(Arrays.asList(
            IMAGE_WIDTH,
            IMAGE_HEIGHT
    ));

    @Override
    public String getNodeType() {
        return NT_IMAGE;
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);
        response.put("width", node.getProperty(IMAGE_WIDTH).getLong());
        response.put("height", node.getProperty(IMAGE_HEIGHT).getLong());
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                   final Property property) throws RepositoryException {
        if (skipProperties.contains(property.getName())) {
            return true;
        }
        return super.skipProperty(context, propertyType, property);
    }
}

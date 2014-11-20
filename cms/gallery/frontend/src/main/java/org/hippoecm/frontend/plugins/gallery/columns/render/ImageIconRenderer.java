/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.columns.render;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNodeType;

public class ImageIconRenderer extends IconRenderer {

    @Override
    protected HippoIcon getIcon(String id, Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            // TODO: replace BULLET_LARGE with image.svg icon
            return new HippoIcon(id, Icon.BULLET_LARGE);
        }
        return super.getIcon(id, node);
    }
}

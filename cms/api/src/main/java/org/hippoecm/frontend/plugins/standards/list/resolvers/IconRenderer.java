/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconRenderer implements IListCellRenderer<Node> {

    static final Logger log = LoggerFactory.getLogger(IconRenderer.class);

    private IconSize size;

    public IconRenderer() {}

    public IconRenderer(final IconSize size) {
        this.size = size;
    }

    public Component getRenderer(String id, IModel<Node> model) {
        final Node node = model.getObject();
        if (node == null) {
            log.warn("Using default icon for unknown node");
            return defaultIcon(id);
        }
        try {
            return getIcon(id, node);
        } catch (RepositoryException e) {
            log.warn("Unable to determine icon for node '{}', using default icon instead",
                    JcrUtils.getNodePathQuietly(node), e);
            return defaultIcon(id);
        }
    }

    private HippoIcon defaultIcon(final String id) {
        return HippoIcon.fromSprite(id, Icon.EMPTY);
    }

    @Override
    public IObservable getObservable(IModel<Node> model) {
        return null;
    }

    protected HippoIcon getIcon(final String id, final Node node) throws RepositoryException {
        return IconRenderUtil.getDocumentOrFolderIcon(id, node, size);
    }
}

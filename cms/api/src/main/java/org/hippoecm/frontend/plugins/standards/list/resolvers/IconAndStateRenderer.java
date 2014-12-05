/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconAndStateRenderer extends AbstractNodeRenderer {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(IconAndStateRenderer.class);

    public Component getViewer(final String id, final Node node) {
        if (node == null) {
            log.warn("Using default icon for unknown node");
            return defaultTypeIcon(id);
        } else {
            return new Container(id, node);
        }
    }

    private static HippoIcon defaultTypeIcon(final String id) {
        return HippoIcon.fromSprite(id, Icon.EMPTY_SMALL);
    }

    private static class Container extends Panel implements IDetachable {

        public Container(final String id, final Node node) {
            super(id, new JcrNodeModel(node));

            add(getTypeIcon("typeIcon"));
            add(getStateIcon("stateIcon"));
        }

        @Override
        protected void onBeforeRender() {
            if (hasBeenRendered()) {
                replace(getStateIcon("stateIcon"));
            }
            super.onBeforeRender();
        }

        private HippoIcon getTypeIcon(final String id) {
            final Node node = getNodeModel().getNode();
            try {
                return IconRenderUtil.getIcon(id, node);
            } catch (RepositoryException e) {
                log.warn("Unable to determine icon for node '{}', using default icon instead",
                        JcrUtils.getNodePathQuietly(node), e);
                return defaultTypeIcon(id);
            }
        }

        private JcrNodeModel getNodeModel() {
            return (JcrNodeModel)getDefaultModel();
        }

        private Component getStateIcon(final String id) {
            final StateIconAttributes stateIconAttributes = new StateIconAttributes(getNodeModel());
            final Icon stateIcon = stateIconAttributes.getIcon();

            if (stateIcon != null) {
                return HippoIcon.inline(id, stateIcon);
            } else {
                EmptyPanel noIcon = new EmptyPanel(id);
                noIcon.setRenderBodyOnly(true);
                return noIcon;
            }
        }

    }

}

/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.editor;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HstReferenceEditor extends Panel {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(HstReferenceEditor.class);

    protected static final String PROPERTY_HST_TEMPLATE = "hst:template";
    protected static final String PROPERTY_HST_COMPONENTCONFIGURATIONID = "hst:componentconfigurationid";
    protected static final String PROPERTY_HST_REFERENCECOMPONENT = "hst:referencecomponent";

    private static final String NODE_HST_CONFIGURATION = "hst:configuration";
    private static final String NODE_HST_WORKSPACE = "hst:workspace";
    private static final String NODE_HST_TEMPLATES = "hst:templates";
    private static final String NODE_HST_CONTAINERCOMPONENTREFERENCE = "hst:containercomponentreference";
    private static final String PROPERY_HST_INHERITSFROM = "hst:inheritsfrom";
    private static final String PROPERTY_HST_RENDERPATH = "hst:renderpath";
    private static final String PROPERTY_HST_SCRIPT = "hst:script";
    private static final String PROPERTY_HST_COMPONENTCLASSNAME = "hst:componentclassname";
    private static final String PROPERTY_HST_XTYPE = "hst:xtype";
    private static final String PATH_PREFIX_WORKSPACE_CONTAINERS = "hst:workspace/hst:containers/";
    private static final String PATH_HST_DEFAULT = "/hst:hst/hst:configurations/hst:default";

    HstReferenceEditor(String id, JcrPropertyModel propertyModel, IModel<String> valueModel) {
        super(id);
        setOutputMarkupId(true);

        final ReferenceLink referenceLink = new ReferenceLink("reference-link", propertyModel, valueModel);
        add(referenceLink);

        // input field
        final TextFieldWidget editor = new TextFieldWidget("reference-edit", valueModel) {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                referenceLink.load();
                target.add(HstReferenceEditor.this);
            }
        };
        editor.setSize("40");
        add(editor);
    }

    private static class ReferenceLink extends AjaxLink<String> {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unused")
        private String linkText;
        private JcrNodeModel linkModel;
        private final JcrPropertyModel propertyModel;

        public ReferenceLink(final String id, final JcrPropertyModel propertyModel, final IModel<String> valueModel) {
            super(id, valueModel);
            this.propertyModel = propertyModel;

            load();
            add(new Label("reference-link-text", new PropertyModel<String>(this, "linkText")));
            add(new AttributeAppender("style", new AbstractReadOnlyModel<Object>() {
                @Override
                public Object getObject() {
                    return !isValidLink() ? "color:blue" : "";
                }
            }, " "));
        }

        private void load() {
            linkText = null;
            linkModel = null;

            try {
                final Node targetNode = getHstReferencedNode();
                linkModel = new JcrNodeModel(targetNode);
                linkText = targetNode.getPath();
                final String title = determineTitle(targetNode);
                if (StringUtils.isNotBlank(title)) {
                    this.add(new AttributeModifier("title", title));
                }
            } catch (PathNotFoundException e) {
                linkText = "(Reference not found. Might be used in inheriting structure though.)";
            } catch (RepositoryException e) {
                linkText = "Repository Exception: " + e.getMessage();
                log.error("Error loading target node by reference " + getModelObject());
            }
        }

        @Override
        protected boolean isLinkEnabled() {
            return isValidLink();
        }

        private boolean isValidLink() {
            return linkModel != null;
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {
            if (linkModel != null) {
                findParent(EditorPlugin.class).setDefaultModel(linkModel);
            }
        }

        @Override
        protected void onDetach() {
            if (linkModel != null) {
                linkModel.detach();
            }
            super.onDetach();
        }


        /**
         * Inspect the target node to see if some info from that node can be used as link title
         *
         * @param targetNode node that the link points to
         * @throws RepositoryException for any unexpected repository problem
         */
        private String determineTitle(final Node targetNode) throws RepositoryException {
            switch (propertyModel.getProperty().getName()) {
                case PROPERTY_HST_TEMPLATE:
                    if (targetNode.hasProperty(PROPERTY_HST_RENDERPATH)) {
                        return targetNode.getProperty(PROPERTY_HST_RENDERPATH).getString();
                    } else if (targetNode.hasProperty(PROPERTY_HST_SCRIPT)) {
                        return "Template contains script";
                    }
                case PROPERTY_HST_COMPONENTCONFIGURATIONID:
                case PROPERTY_HST_REFERENCECOMPONENT:
                    if (targetNode.hasProperty(PROPERTY_HST_COMPONENTCLASSNAME)) {
                        return targetNode.getProperty(PROPERTY_HST_COMPONENTCLASSNAME).getString();
                    } else if (targetNode.hasProperty(PROPERTY_HST_XTYPE)) {
                        return targetNode.getProperty(PROPERTY_HST_XTYPE).getString();
                    }
            }
            return null;
        }

        /**
         * Get the hst configuration node that a hst property refers to
         *
         * @return the requested node
         * @throws javax.jcr.PathNotFoundException when the referenced node cannot be found
         * @throws javax.jcr.RepositoryException   for any unexpected repository problem
         */
        private Node getHstReferencedNode() throws RepositoryException {

            final String propertyValue = getModelObject();
            // first try: hst configuration nodes in the current hst:workspace or hst:configuration group
            Node currentHstConfiguration = propertyModel.getProperty().getParent();
            Node root = currentHstConfiguration.getSession().getRootNode();
            Node templateNode;
            do {
                if (currentHstConfiguration.getPrimaryNodeType().isNodeType(NODE_HST_WORKSPACE)) {
                    templateNode = getConfigurationNode(currentHstConfiguration, propertyValue);
                    if (templateNode != null) {
                        return templateNode;
                    }
                } else if (currentHstConfiguration.getPrimaryNodeType().isNodeType(NODE_HST_CONFIGURATION)) {
                    templateNode = getConfigurationNode(currentHstConfiguration, propertyValue);
                    if (templateNode != null) {
                        return templateNode;
                    } else {
                        break;
                    }
                }
                currentHstConfiguration = currentHstConfiguration.getParent();
            } while (!root.isSame(currentHstConfiguration));

            // second try: hst configuration nodes in any inheritsfrom hst:configuration group
            if (currentHstConfiguration.hasProperty(PROPERY_HST_INHERITSFROM)) {
                final Value[] inheritFromPaths = currentHstConfiguration.getProperty(PROPERY_HST_INHERITSFROM).getValues();
                for (Value inheritsFromPath : inheritFromPaths) {
                    Node inheritedHstConfiguration = currentHstConfiguration.getNode(inheritsFromPath.getString());
                    templateNode = getConfigurationNode(inheritedHstConfiguration, propertyValue);
                    if (templateNode != null) {
                        return templateNode;
                    }
                }
            }

            // third try: hst configuration nodes from hst:default group
            final Node hstDefaultConfiguration = UserSession.get().getJcrSession().getNode(PATH_HST_DEFAULT);
            if (hstDefaultConfiguration.hasNode(NODE_HST_TEMPLATES)) {
                templateNode = getConfigurationNode(hstDefaultConfiguration, propertyValue);
                if (templateNode != null) {
                    return templateNode;
                }
            }

            throw new PathNotFoundException();
        }

        /**
         * Get a hst configuration node
         *
         * @param hstConfiguration a hst:configuration node that may have a named hst configuration node
         * @param nodeName         the name of the configuration node to be retrieved
         * @return the requested node or null
         * @throws javax.jcr.RepositoryException for any unexpected repository problem
         */
        private Node getConfigurationNode(final Node hstConfiguration, String nodeName) throws RepositoryException {
            StringBuilder relPath = new StringBuilder();
            addPathPrefix(relPath);
            relPath.append(nodeName);
            if (hstConfiguration.hasNode(relPath.toString())) {
                return hstConfiguration.getNode(relPath.toString());
            }
            return null;
        }

        /**
         * Add a default path prefix if necessary
         *
         * @param relPath path to append the prexif to
         * @throws RepositoryException for any unexpected repository problem
         */
        private void addPathPrefix(final StringBuilder relPath) throws RepositoryException {
            // references from hst:template properties are always to hst:templates nodes
            final boolean isHstTemplate = propertyModel.getProperty().getName().equals(PROPERTY_HST_TEMPLATE);
            if (isHstTemplate) {
                relPath.append(NODE_HST_TEMPLATES);
                relPath.append("/");
                return;
            }

            // references from hst:containercomponentreference nodes are always to hst:workspace/hst:container nodes
            final boolean isContainerComponentReference = propertyModel.getProperty().getParent().getPrimaryNodeType().isNodeType(NODE_HST_CONTAINERCOMPONENTREFERENCE);
            if (isContainerComponentReference) {
                relPath.append(PATH_PREFIX_WORKSPACE_CONTAINERS);
            }
        }

    }

}

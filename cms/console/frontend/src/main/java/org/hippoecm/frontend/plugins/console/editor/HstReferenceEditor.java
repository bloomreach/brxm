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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;

class HstReferenceEditor extends Panel {
    private static final long serialVersionUID = 1L;

    protected static final String PROPERTY_HST_TEMPLATE = "hst:template";
    protected static final String PROPERTY_HST_COMPONENTCONFIGURATIONID = "hst:componentconfigurationid";
    protected static final String PROPERTY_HST_REFERENCECOMPONENT = "hst:referencecomponent";

    private static final String NODE_HST_CONFIGURATION = "hst:configuration";
    private static final String NODE_HST_WORKSPACE = "hst:workspace";
    private static final String NODE_HST_TEMPLATES = "hst:templates";
    private static final String NODE_JCR_ROOT = "rep:root";
    private static final String NODE_HST_CONTAINERCOMPONENTREFERENCE = "hst:containercomponentreference";
    private static final String PROPERY_HST_INHERITSFROM = "hst:inheritsfrom";
    private static final String PROPERTY_HST_RENDERPATH = "hst:renderpath";
    private static final String PROPERTY_HST_SCRIPT = "hst:script";
    private static final String PROPERTY_HST_COMPONENTCLASSNAME = "hst:componentclassname";
    private static final String PROPERTY_HST_XTYPE = "hst:xtype";
    private static final String PATH_PREFIX_WORKSPACE_CONTAINERS = "hst:workspace/hst:containers/";
    private static final String PATH_HST_DEFAULT = "/hst:hst/hst:configurations/hst:default";

    HstReferenceEditor(String id, JcrPropertyModel propertyModel, JcrPropertyValueModel valueModel) {
        super(id);
        try {
            Node targetNode = getHstReferencedNode(propertyModel, valueModel);

            // link to referenced node
            AjaxLink link = new AjaxLink("reference-link", new JcrNodeModel(targetNode)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget requestTarget) {
                    EditorPlugin plugin = (EditorPlugin) findParent(EditorPlugin.class);
                    plugin.setDefaultModel((JcrNodeModel) getModel());
                }
            };
            add(link);
            addLinkTitle(link, targetNode, propertyModel.getProperty().getName());
            link.add(new Label("reference-link-text", new Model(targetNode.getPath())));

            // input field
            TextFieldWidget editor = new TextFieldWidget("reference-edit", valueModel);
            editor.setSize("40");
            add(editor);

        } catch (PathNotFoundException e) {
            TextFieldWidget editor = new TextFieldWidget("reference-edit", valueModel);
            editor.setSize("40");
            add(editor);

            DisabledLink link = new DisabledLink("reference-link", new Model("Reference might be inherited"));
            link.add(new AttributeAppender("style", new Model("color:blue"), " "));
            add(link);

        } catch (RepositoryException e) {
            add(new Label("reference-edit", e.getClass().getName()));
            add(new DisabledLink("reference-link", new Model(e.getMessage())));
        }
    }

    /**
     * Inspect the target node to see if some info from that node can be added as link title
     *
     * @param link the link to add the title to
     * @param targetNode node that the link points to
     * @param propertyName name of the property shown
     * @throws RepositoryException for any unexpected repository problem
     */
    private void addLinkTitle(final AjaxLink link, final Node targetNode, final String propertyName) throws RepositoryException {
        String title = null;
        if (propertyName.equals(PROPERTY_HST_TEMPLATE)) {
            if(targetNode.hasProperty(PROPERTY_HST_RENDERPATH)) {
                title = targetNode.getProperty(PROPERTY_HST_RENDERPATH).getString();
            } else if (targetNode.hasProperty(PROPERTY_HST_SCRIPT)) {
                title = "Template contains script";

            }
        } else if(propertyName.equals(PROPERTY_HST_COMPONENTCONFIGURATIONID) || propertyName.equals(PROPERTY_HST_REFERENCECOMPONENT)) {
            if(targetNode.hasProperty(PROPERTY_HST_COMPONENTCLASSNAME)) {
                title = targetNode.getProperty(PROPERTY_HST_COMPONENTCLASSNAME).getString();
            } else if (targetNode.hasProperty(PROPERTY_HST_XTYPE)) {
                title = targetNode.getProperty(PROPERTY_HST_XTYPE).getString();
            }
        }
        if(StringUtils.isNotBlank(title)) {
            link.add(new AttributeModifier("title", title));
        }
    }

    /**
     * Get the hst configuration node that a hst property refers to
     *
     * @param propertyModel model representing the property
     * @param valueModel model representing the value of the property
     * @return the requested node or null
     * @throws javax.jcr.RepositoryException for any unexpected repository problem
     */
    private Node getHstReferencedNode(final JcrPropertyModel propertyModel, final JcrPropertyValueModel valueModel) throws RepositoryException {
        String propertyValue = valueModel.getValue().getString();

        // first try: hst configuration nodes in the current hst:workspace or hst:configuration group
        Node currentHstConfiguration = propertyModel.getProperty().getParent();
        Node templateNode;
        do {
            if(currentHstConfiguration.getPrimaryNodeType().isNodeType(NODE_HST_WORKSPACE)) {
                templateNode = getConfigurationNode(currentHstConfiguration, propertyValue, propertyModel);
                if(templateNode != null) {
                    return templateNode;
                }
            } else if(currentHstConfiguration.getPrimaryNodeType().isNodeType(NODE_HST_CONFIGURATION)) {
                templateNode = getConfigurationNode(currentHstConfiguration, propertyValue, propertyModel);
                if(templateNode != null) {
                    return templateNode;
                } else {
                    break;
                }
            }
            currentHstConfiguration = currentHstConfiguration.getParent();
        } while (!currentHstConfiguration.getPrimaryNodeType().isNodeType(NODE_JCR_ROOT));

        // second try: hst configuration nodes in any inheritsfrom hst:configuration group
        if(currentHstConfiguration.hasProperty(PROPERY_HST_INHERITSFROM)) {
            final Value[] inheritFromPaths = currentHstConfiguration.getProperty(PROPERY_HST_INHERITSFROM).getValues();
            for(Value inheritsFromPath : inheritFromPaths) {
                Node inheritedHstConfiguration = currentHstConfiguration.getNode(inheritsFromPath.getString());
                templateNode = getConfigurationNode(inheritedHstConfiguration, propertyValue, propertyModel);
                if(templateNode != null) {
                    return templateNode;
                }
            }
        }

        // third try: hst configuration nodes from hst:default group
        final Node hstDefaultConfiguration = UserSession.get().getJcrSession().getNode(PATH_HST_DEFAULT);
        if(hstDefaultConfiguration.hasNode(NODE_HST_TEMPLATES)) {
            templateNode = getConfigurationNode(hstDefaultConfiguration, propertyValue, propertyModel);
            if(templateNode != null) {
                return templateNode;
            }
        }

        throw new PathNotFoundException();
    }

    /**
     * Get a hst configuration node
     *
     * @param hstConfiguration a hst:configuration node that may have a named hst configuration node
     * @param nodeName the name of the configuration node to be retrieved
     * @param propertyModel model representing the property
     * @return the requested node or null
     * @throws javax.jcr.RepositoryException for any unexpected repository problem
     */
    private Node getConfigurationNode(final Node hstConfiguration, String nodeName, final JcrPropertyModel propertyModel) throws RepositoryException {
        StringBuilder relPath = new StringBuilder();
        addPathPrefix(relPath, propertyModel);
        relPath.append(nodeName);
        if(hstConfiguration.hasNode(relPath.toString())) {
           return hstConfiguration.getNode(relPath.toString());
        }
        return null;
    }

    /**
     * Add a default path prefix if necessary
     *
     * @param relPath path to append the prexif to
     * @param propertyModel model representing the property
     * @throws RepositoryException for any unexpected repository problem
     */
    private void addPathPrefix(final StringBuilder relPath, final JcrPropertyModel propertyModel) throws RepositoryException {
        // references from hst:template properties are always to hst:templates nodes
        final boolean isHstTemplate = propertyModel.getProperty().getName().equals(PROPERTY_HST_TEMPLATE);
        if(isHstTemplate) {
            relPath.append(NODE_HST_TEMPLATES);
            relPath.append("/");
            return;
        }

        // references from hst:containercomponentreference nodes are always to hst:workspace/hst:container nodes
        final boolean isContainerComponentReference = propertyModel.getProperty().getParent().getPrimaryNodeType().isNodeType(NODE_HST_CONTAINERCOMPONENTREFERENCE);
        if(isContainerComponentReference) {
            relPath.append(PATH_PREFIX_WORKSPACE_CONTAINERS);
        }
    }

    private class DisabledLink extends AjaxLink {
        private static final long serialVersionUID = 1L;

        public DisabledLink(String id, IModel linktext) {
            super(id);
            setEnabled(false);
            add(new Label("reference-link-text", linktext));
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
        }
    }

}

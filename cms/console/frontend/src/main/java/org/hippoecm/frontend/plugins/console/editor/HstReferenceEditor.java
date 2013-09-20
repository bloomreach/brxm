/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
    private static final String NODE_HST_CONFIGURATIONS = "hst:configurations";
    private static final String NODE_HST_DEFAULT = "hst:default";
    private static final String NODE_HST_TEMPLATES = "hst:templates";
    private static final String PROPERY_HST_INHERITSFROM = "hst:inheritsfrom";
    private static final String PROPERTY_HST_RENDERPATH = "hst:renderpath";
    private static final String PROPERTY_HST_SCRIPT = "hst:script";
    private static final String PROPERTY_HST_COMPONENTCLASSNAME = "hst:componentclassname";

    HstReferenceEditor(String id, JcrPropertyModel propertyModel, JcrPropertyValueModel valueModel) {
        super(id);
        try {
            final boolean isHstTemplate = propertyModel.getProperty().getName().equals(PROPERTY_HST_TEMPLATE);

            String stringValue = valueModel.getValue().getString();
            Node targetNode = getHstReferencedNode(propertyModel, stringValue, isHstTemplate);

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

            DisabledLink link = new DisabledLink("reference-link", new Model(getString("reference-not-found")));
            link.add(new AttributeAppender("style", new Model("color:red"), " "));
            add(link);

        } catch (RepositoryException e) {
            add(new Label("reference-edit", e.getClass().getName()));
            add(new DisabledLink("reference-link", new Model(e.getMessage())));
        }
    }

    private void addLinkTitle(final AjaxLink link, final Node targetNode, final String propertyName) throws RepositoryException {
        if (propertyName.equals(PROPERTY_HST_TEMPLATE)) {
            if(targetNode.hasProperty(PROPERTY_HST_RENDERPATH)) {
                link.add(new AttributeModifier("title", targetNode.getProperty(PROPERTY_HST_RENDERPATH).getString()));
            } else if (targetNode.hasProperty(PROPERTY_HST_SCRIPT)) {
                link.add(new AttributeModifier("title", getString("template-has-script")));
            }
        } else if(propertyName.equals(PROPERTY_HST_COMPONENTCONFIGURATIONID) || propertyName.equals(PROPERTY_HST_REFERENCECOMPONENT)) {
            if(targetNode.hasProperty(PROPERTY_HST_COMPONENTCLASSNAME)) {
                link.add(new AttributeModifier("title", targetNode.getProperty(PROPERTY_HST_COMPONENTCLASSNAME).getString()));
            }
        }
    }

    /**
     * Get the hst:template node that a hst:template property refers to
     *
     * @param propertyModel model representing the hst:template property
     * @param templateName the value of the property i.e. the name of the template node
     * @param isHstTemplate true if the reference is to a hst:template node
     * @return the requested node or null
     * @throws javax.jcr.RepositoryException for any unexpected repository problem
     */
    private Node getHstReferencedNode(final JcrPropertyModel propertyModel, final String templateName, final boolean isHstTemplate) throws RepositoryException {
        final Node rootNode = UserSession.get().getJcrSession().getRootNode();

        // first try: hst:templates in the current hst:configuration group
        Node currentHstConfiguration = propertyModel.getProperty().getParent();
        do {
            if(currentHstConfiguration.getPrimaryNodeType().isNodeType(NODE_HST_CONFIGURATION)) {
                break;
            }
            currentHstConfiguration = currentHstConfiguration.getParent();
        } while (!currentHstConfiguration.equals(rootNode));

        Node templateNode = getTemplateNode(currentHstConfiguration, templateName, isHstTemplate);
        if(templateNode != null) {
            return templateNode;
        }

        // second try: hst:templates in any inheritsfrom hst:configuration group
        if(currentHstConfiguration.hasProperty(PROPERY_HST_INHERITSFROM)) {
            final Value[] inheritFromPaths = currentHstConfiguration.getProperty(PROPERY_HST_INHERITSFROM).getValues();
            for(Value inheritsFromPath : inheritFromPaths) {
                Node inheritedHstConfiguration = currentHstConfiguration.getNode(inheritsFromPath.getString());
                templateNode = getTemplateNode(inheritedHstConfiguration, templateName, isHstTemplate);
                if(templateNode != null) {
                    return templateNode;
                }
            }
        }

        // third try: hst:templates from hst:default group
        Node hstDefaultConfiguration = currentHstConfiguration.getParent();
        do {
            if(hstDefaultConfiguration.getPrimaryNodeType().isNodeType(NODE_HST_CONFIGURATIONS)) {
                hstDefaultConfiguration = hstDefaultConfiguration.getNode(NODE_HST_DEFAULT);
                break;
            }
            hstDefaultConfiguration = hstDefaultConfiguration.getParent();
        } while (!hstDefaultConfiguration.equals(rootNode));

        if(hstDefaultConfiguration.hasNode(NODE_HST_TEMPLATES)) {
            templateNode = getTemplateNode(hstDefaultConfiguration, templateName, isHstTemplate);
            if(templateNode != null) {
                return templateNode;
            }
        }

        throw new PathNotFoundException();
    }

    /**
     * Get a hst:template subnode of the hst:templates node
     *
     * @param hstConfiguration a hst:configuration node that may have a named hst:template node
     * @param templateName the name of the template node to be retrieved
     * @param isHstTemplate true if the reference is to a hst:template node
     * @return the requested node or null
     * @throws javax.jcr.RepositoryException for any unexpected repository problem
     */
    private Node getTemplateNode(final Node hstConfiguration, String templateName, final boolean isHstTemplate) throws RepositoryException {
        StringBuilder relPath = new StringBuilder();
        if(isHstTemplate) {
            relPath.append(NODE_HST_TEMPLATES);
            relPath.append("/");
        }
        relPath.append(templateName);
        if(hstConfiguration.hasNode(relPath.toString())) {
           return hstConfiguration.getNode(relPath.toString());
        }
        return null;
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

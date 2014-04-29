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

import java.util.regex.Pattern;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReferenceEditor extends Panel {
    private static final long serialVersionUID = 1L;
    
    public static final Logger log = LoggerFactory.getLogger(ReferenceEditor.class);

    ReferenceEditor(String id, JcrPropertyModel propertyModel, final JcrPropertyValueModel<String> valueModel) {
        super(id, valueModel);
        setOutputMarkupId(true);
        
        final ReferenceLink referenceLink = new ReferenceLink("reference-link", valueModel);
        add(referenceLink);
        try {
            if (propertyModel.getProperty().getDefinition().isProtected()) {
                add(new Label("reference-edit", valueModel.getObject()));
            } else {
                TextFieldWidget editor = new TextFieldWidget("reference-edit", valueModel) {
                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        referenceLink.load();
                        target.add(ReferenceEditor.this);
                    }
                };
                editor.setSize("40");
                add(editor);
            }
        } catch (RepositoryException e) {
            addOrReplace(new Label("reference-edit", e.getClass().getName()));
            addOrReplace(new ExceptionLink("reference-link", Model.of(e.getMessage())));
        }
    }

    private static class ReferenceLink extends AjaxLink<String> {
        private static final long serialVersionUID = 1L;

        private static Pattern JCR_IDENTIFIER = Pattern.compile("^\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}$");
        
        @SuppressWarnings("unused")
        private String linkText = null;
        private JcrNodeModel linkModel = null;
        
        public ReferenceLink(String id, IModel<String> valueModel) {
            super(id, valueModel);
            load();
            
            add(new Label("reference-link-text", new PropertyModel<String>(this, "linkText")));
            add(new AttributeAppender("style", new AbstractReadOnlyModel<Object>() {
                @Override
                public Object getObject() {
                    return !isValidLink() ? "color:red" : "";
                }
            }, " "));
        }

        @Override
        protected boolean isLinkEnabled() {
            return isValidLink();
        }

        private boolean isValidLink() {
            return linkModel != null;
        }

        //load uuid from parent model and (re)set the linkModel+text if uuid is valid
        private void load() {
            linkText = null;
            linkModel = null;

            String uuid = getModelObject();
            if (JCR_IDENTIFIER.matcher(uuid).matches()) {
                try {
                    Session session = UserSession.get().getJcrSession();
                    Node targetNode = session.getNodeByIdentifier(uuid);
                    linkText = targetNode.getPath();
                    linkModel = new JcrNodeModel(targetNode);
                } catch (ItemNotFoundException e) {
                    linkText = getString("reference.editor.input.not-found");
                } catch (RepositoryException e) {
                    linkText = getString("reference.editor.input.error");
                    log.error("Error loading node with uuid " + uuid, e);
                }
            } else {
                linkText = getString("reference.editor.input.invalid");
            }
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
    }

    private static class ExceptionLink extends AjaxLink {
        private static final long serialVersionUID = 1L;

        public ExceptionLink(String id, IModel linktext) {
            super(id);
            setEnabled(false);
            add(new Label("reference-link-text", linktext));
            add(new AttributeAppender("style", Model.of("color:red"), " "));
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
        }
    }
}

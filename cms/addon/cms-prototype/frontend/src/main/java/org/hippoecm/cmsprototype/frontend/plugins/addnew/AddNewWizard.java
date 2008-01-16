/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.addnew;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.MessageContext;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Form to create new documents.
 * It gets the available document templates from the configuration in the repository.
 *
 */
public class AddNewWizard extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AddNewWizard.class);

    public AddNewWizard(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        final AddNewForm form = new AddNewForm("addNewForm");
        add(new FeedbackPanel("feedback"));

        form.add(new AjaxEventBehavior("onsubmit") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                // FIXME save ajax request target so it can be used in onSubmit()
                form.setTarget(target);
            }

        });

        add(form);

    }

    private final class AddNewForm extends Form {
        private static final long serialVersionUID = 1L;

        private ValueMap properties;
        transient private AjaxRequestTarget target;

        public AddNewForm(String id) {
            super(id);
            properties = new ValueMap();
            TextField name = new TextField("name", new PropertyModel(properties, "name"));
            name.setRequired(true);
            add(name);

            List<String> templates = getTemplates();
            DropDownChoice template = new DropDownChoice("template", new PropertyModel(properties, "template"),
                    templates);
            template.setRequired(true);
            add(template);

            add(new Button("submit", new Model("Add")));

        }

        public void setTarget(AjaxRequestTarget target) {
            this.target = target;
        }

        @Override
        protected void onSubmit() {
            Node doc = createDocument();

            if (doc != null && target != null) {
                Channel channel = getDescriptor().getIncoming();
                if (channel != null) {

                    Request request = channel.createRequest("flush", getNodeModel().findRootModel()
                            .getMapRepresentation());
                    MessageContext context = request.getContext();
                    channel.send(request);

                    JcrNodeModel model = new JcrNodeModel(doc);
                    request = channel.createRequest("select", model.getMapRepresentation());
                    request.setContext(context);
                    channel.send(request);

                    request = channel.createRequest("browse", model.getMapRepresentation());
                    request.setContext(context);
                    channel.send(request);

                    context.apply(target);
                }
            }

            properties.clear();

        }

        private List<String> getTemplates() {
            List<String> templates = new ArrayList<String>();
            UserSession session = (UserSession) Session.get();

            try {
                QueryManager queryManager = session.getJcrSession().getWorkspace().getQueryManager();
                NodeTypeManager ntMgr = session.getJcrSession().getWorkspace().getNodeTypeManager();

                String xpath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH + "/"
                        + session.getHippo() + "/*/" + HippoNodeType.HIPPO_TEMPLATES + "/*";

                Query query = queryManager.createQuery(xpath, Query.XPATH);
                QueryResult result = query.execute();
                NodeIterator iterator = result.getNodes();
                while (iterator.hasNext()) {
                    Node node = iterator.nextNode();
                    try {
                        NodeType type = ntMgr.getNodeType(node.getName());
                        if (type.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                            templates.add(node.getName());
                        }
                    } catch (NoSuchNodeTypeException ex) {
                        log.warn("Template " + node.getName() + " does not correspond to a node type");
                    }
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }

            return templates;
        }

        private Node createDocument() {
            UserSession session = (UserSession) Session.get();
            Node result = null;
            String name = (String) properties.get("name");
            String type = (String) properties.get("template");

            try {
                Node rootNode = session.getRootNode();
                Node typeNode;
                if (rootNode.hasNode(type)) {
                    typeNode = rootNode.getNode(type);
                } else {
                    typeNode = rootNode.addNode(type, "nt:unstructured");
                }
                Node handle = typeNode.addNode(name, HippoNodeType.NT_HANDLE);

                // save the created nodes
                session.getJcrSession().save();

                // find template node describing the node type
                QueryManager queryManager = session.getJcrSession().getWorkspace().getQueryManager();
                String xpath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH + "/"
                        + session.getHippo() + "/*/" + HippoNodeType.HIPPO_TEMPLATES + "/"
                        + (String) properties.get("template");
                Query query = queryManager.createQuery(xpath, Query.XPATH);
                NodeIterator templateIterator = query.execute().getNodes();
                if (templateIterator.getSize() != 1) {
                    log.error("Found " + templateIterator.getSize() + " matching templates, expected one.");
                    return result;
                }
                Node template = templateIterator.nextNode();
                if(template.hasNode("hippo:prototype")) {
                    Node prototype = template.getNode("hippo:prototype");
                    Workspace workspace = ((UserSession) Session.get()).getJcrSession().getWorkspace();
                    workspace.copy(prototype.getPath(), handle.getPath() + "/" + name);
                } else {
                    handle.addNode(name, type);
                    handle.save();
                }

                result = handle.getNode(name);

            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }

            return result;
        }

    }

}

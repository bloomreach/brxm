/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor.plugins;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.version.VersionException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoQueryTemplatePlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(HippoQueryTemplatePlugin.class);

    private final String mode;
    private String language;
    private String statement;
    private String incorrectquery = "";
    private Label incorrectqueryLabel;

    public HippoQueryTemplatePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        JcrNodeModel jcrNodeModel = (JcrNodeModel) getDefaultModel();
        Node queryNode = jcrNodeModel.getNode();
        try {

            //if(!queryNode.isNodeType("nt:query"))

            if (!queryNode.hasProperty("jcr:language")) {
                queryNode.setProperty("jcr:language", "xpath");
            }
            if (!queryNode.hasProperty("jcr:statement")) {
                queryNode.setProperty("jcr:statement", "//*");
            }

            QueryManager qrm = queryNode.getSession().getWorkspace().getQueryManager();
            try {
                Query query = qrm.getQuery(queryNode);
                language = query.getLanguage();
                statement = query.getStatement();
            } catch (InvalidQueryException e) {
                log.warn("Invalid query statement. Revert to default xpath statement //*");
                language = "xpath";
                statement = "//*";
                queryNode.setProperty("jcr:statement", "//*");
                queryNode.setProperty("jcr:language", "xpath");
            }

        } catch (ValueFormatException e) {
            log.error(e.getMessage());
        } catch (PathNotFoundException e) {
            log.error(e.getMessage());
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        mode = config.getString("mode", "view");
        Fragment fragment = new Fragment("fragment", mode, this);
        add(fragment);

        if ("edit".equals(mode)) {
            fragment.add(new TextFieldWidget("language", new PropertyModel(this, "language")) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    storeQueryAsNode(target);
                }
            });

            fragment.add(new TextFieldWidget("statement", new PropertyModel(this, "statement")) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    storeQueryAsNode(target);
                }
            });
            incorrectqueryLabel = new Label("incorrectquery", new PropertyModel(this, "incorrectquery"));
            incorrectqueryLabel.setOutputMarkupId(true);
            fragment.add(incorrectqueryLabel);
        } else {
            fragment.add(new Label("value", statement + " (" + language + ")"));
        }
        setOutputMarkupId(true);
    }

    private void storeQueryAsNode(AjaxRequestTarget target) {
        Node queryNode = ((JcrNodeModel) getDefaultModel()).getNode();

        try {
            Node parentNode = queryNode.getParent();
            String nodeName = queryNode.getName();

            Session session = queryNode.getSession();

            QueryManager qrm = session.getWorkspace().getQueryManager();
            if (statement == null) {
                throw new InvalidQueryException("statement is not allowed to be empty");
            }

            if (language == null) {
                throw new InvalidQueryException("supported languages are 'xpath' and 'sql'");
            }
            language = language.toLowerCase();

            Query query = qrm.createQuery(statement, language);

            /*
             * you cannot directly use storeAsNode again for with the same path, because
             * that result in an item exists exception. The only way is to keep the property
             * values in memory, remove the node, and store is again
             */
            getDefaultModel().detach();
            queryNode.remove();
            if (query instanceof HippoQuery) {
                ((HippoQuery) query).storeAsNode(parentNode.getPath() + "/" + nodeName, HippoNodeType.NT_QUERY);
            } else {
                query.storeAsNode(parentNode.getPath() + "/" + nodeName);
            }

            incorrectquery = "";
            target.addComponent(incorrectqueryLabel);

        } catch (InvalidQueryException e) {
            logAndInform(target, e);
        } catch (ValueFormatException e) {
            logAndInform(target, e);
        } catch (VersionException e) {
            logAndInform(target, e);
        } catch (LockException e) {
            logAndInform(target, e);
        } catch (ConstraintViolationException e) {
            logAndInform(target, e);
        } catch (RepositoryException e) {
            logAndInform(target, e);
        }

    }

    private void logAndInform(AjaxRequestTarget target, Exception e) {
        if (target != null) {
            incorrectquery = "Incorrect statement: changes won't be saved";
            target.addComponent(incorrectqueryLabel);
            IDialogService dialogService = getDialogService();
            if (dialogService != null) {
                dialogService.show(new ExceptionDialog(new IllegalArgumentException(incorrectquery)));
            }
        }
        log.debug(e.getMessage());
    }

}

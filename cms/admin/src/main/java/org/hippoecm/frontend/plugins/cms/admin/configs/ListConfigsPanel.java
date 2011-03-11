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
package org.hippoecm.frontend.plugins.cms.admin.configs;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel displays a pageable list of users.
 */
public class ListConfigsPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id: ListUsersPanel.java 18459 2009-06-09 13:15:41Z bvdschans $";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ListConfigsPanel.class);

    private final ConfigBackupDataProvider configDataProvider = new ConfigBackupDataProvider();
    private final Form form;
    private final AdminDataTable table;
    private String backupName;

    public ListConfigsPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        //add(new AjaxBreadCrumbPanelLink("create-user", context, this, CreateUserPanel.class));

                // add form with markup id setter so it can be updated via ajax
        form = new Form("form", new CompoundPropertyModel(this));
        form.setOutputMarkupId(true);
        add(form);

        FormComponent fc;

        fc = new RequiredTextField("backupName");
        fc.add(StringValidator.minimumLength(2));
        form.add(fc);

        form.add(new AjaxButton("create-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    HippoSession session = (HippoSession) ((UserSession) Session.get()).getJcrSession();
                    ConfigBackupManager manager = new ConfigBackupManager(session);

                    if (manager.hasConfigBackup(backupName)) {
                        Session.get().warn(getString("backup-create-failed-exists", new CompoundPropertyModel(ListConfigsPanel.this)));
                    } else {
                        log.info("Configuration backup '" + backupName + "' created by " + session.getUserID());
                        manager.createConfigBackup(backupName);
                        session.save();
                        Session.get().info(getString("backup-created", new CompoundPropertyModel(ListConfigsPanel.this)));
                        backupName = "";
                    }
                } catch (RepositoryException e) {
                    Session.get().warn(getString("backup-create-failed", new CompoundPropertyModel(ListConfigsPanel.this)));
                    log.error("Unable to create configuration backup '" + backupName + "' : ", e);
                }
                target.addComponent(ListConfigsPanel.this);
            }
            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                // make sure the feedback panel is shown
                target.addComponent(ListConfigsPanel.this);
            }
        });

        form.add(new AjaxButton("upload-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                Session.get().warn(getString("backup-uploaded"));
                target.addComponent(ListConfigsPanel.this);
            }
        });

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new ResourceModel("configuration-created"), "createdAsString"));
        columns.add(new PropertyColumn(new ResourceModel("configuration-name"), "name"));
        columns.add(new PropertyColumn(new ResourceModel("configuration-createdby"), "createdBy"));
        columns.add(new AbstractColumn(new Model(""), "actions") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item item, final String componentId, final IModel model) {
                final ConfigBackup backup = (ConfigBackup) model.getObject();
                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new ResourceModel("backup-remove-action")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            HippoSession session = (HippoSession) ((UserSession) Session.get()).getJcrSession();
                            ConfigBackupManager manager = new ConfigBackupManager(session);

                            log.info("Removing configuration backup '" + backup.getName() + "' by " + session.getUserID());
                            manager.removeConfigBackup(backup.getName());
                            session.save();
                            Session.get().info(getString("backup-removed"));
                        } catch (RepositoryException e) {
                            Session.get().warn(getString("backup-remove-failed", model));
                            log.error("Unable to remove configuration backup '" + backup.getName() + "' : ", e);
                        }
                        target.addComponent(ListConfigsPanel.this);
                    }
                };
                item.add(action);
            }
        });
        columns.add(new AbstractColumn(new Model(""), "restore") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item item, final String componentId, final IModel model) {
                final ConfigBackup backup = (ConfigBackup) model.getObject();
                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new ResourceModel("backup-restore-action")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            HippoSession session = (HippoSession) ((UserSession) Session.get()).getJcrSession();
                            ConfigBackupManager manager = new ConfigBackupManager(session);

                            log.info("Restoring configuration backup '" + backup.getName() + "' by " + session.getUserID());
                            manager.restoreConfigBackup(backup.getName());
                            session.save();
                            Session.get().info(getString("backup-restored"));
                        } catch (RepositoryException e) {
                            Session.get().warn(getString("backup-restore-failed", model));
                            log.error("Unable to restore configuration backup '" + backup.getName() + "' : ", e);
                        }
                        target.addComponent(ListConfigsPanel.this);
                    }
                };
                item.add(action);
            }
        });
        columns.add(new AbstractColumn(new Model(""), "download") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item item, final String componentId, final IModel model) {
                final ConfigBackup backup = (ConfigBackup) model.getObject();
                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new ResourceModel("backup-download-action")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Session.get().info(getString("backup-downloaded"));
                        target.addComponent(ListConfigsPanel.this);
                    }
                };
                item.add(action);
            }
        });

        table = new AdminDataTable("table", columns, configDataProvider, 20);
        table.setOutputMarkupId(true);
        add(table);
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("admin-configurations-title", component, null);
    }
}

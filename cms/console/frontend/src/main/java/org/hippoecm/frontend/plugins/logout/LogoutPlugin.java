/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.logout;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.SystemInfoDataProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.console.NodeModelReference;
import org.hippoecm.frontend.plugins.console.menu.cnd.CndExportDialog;
import org.hippoecm.frontend.plugins.console.menu.cnd.CndImportDialog;
import org.hippoecm.frontend.plugins.console.menu.content.ContentExportDialog;
import org.hippoecm.frontend.plugins.console.menu.content.ContentImportDialog;
import org.hippoecm.frontend.plugins.console.menu.namespace.NamespaceDialog;
import org.hippoecm.frontend.plugins.console.menu.nodereset.NodeResetDialog;
import org.hippoecm.frontend.plugins.console.menu.patch.ApplyPatchDialog;
import org.hippoecm.frontend.plugins.console.menu.patch.CreatePatchDialog;
import org.hippoecm.frontend.plugins.console.menu.permissions.PermissionsDialog;
import org.hippoecm.frontend.plugins.console.menu.refs.ReferencesDialog;
import org.hippoecm.frontend.plugins.console.menu.systeminfo.SystemInfoDialog;
import org.hippoecm.frontend.plugins.console.menu.workflow.WorkflowDialog;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.service.ILogoutService;
import org.hippoecm.frontend.service.render.ListViewPlugin;

import javax.jcr.Node;

public class LogoutPlugin extends ListViewPlugin<Node> {
    private static SystemInfoDataProvider systemDataProvider = new SystemInfoDataProvider();

    public LogoutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final String username = getSession().getJcrSession().getUserID();
        add(new Label("username", Model.of(username)));

        final ILogoutService logoutService = getPluginContext().getService(ILogoutService.SERVICE_ID, ILogoutService.class);
        add(new LogoutLink("logout-link", logoutService));

        final WebMarkupContainer logo = new WebMarkupContainer("logo");
        logo.add(TitleAttribute.set("Hippo Release Version: " + systemDataProvider.getReleaseVersion()));
        add(logo);

        // xml import
        final IDialogService dialogService = getDialogService();
        IDialogFactory dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public IDialogService.Dialog createDialog() {
                return new ContentImportDialog(new NodeModelReference(LogoutPlugin.this, (JcrNodeModel) getDefaultModel()));
            }
        };
        final DialogLink xmlImport = new DialogLink("xml-import", new Model<>("XML Import"), dialogFactory, dialogService);
        add(xmlImport);

        // xml export
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public IDialogService.Dialog createDialog() {
                return new ContentExportDialog(new NodeModelReference(LogoutPlugin.this, (JcrNodeModel) getDefaultModel()));
            }
        };
        final DialogLink xmlExport = new DialogLink("xml-export", new Model<>("XML Export"), dialogFactory, dialogService);
        add(xmlExport);

        // cnd import
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public IDialogService.Dialog createDialog() {
                return new CndImportDialog();
            }
        };
        final DialogLink cndImport = new DialogLink("cnd-import", new Model<>("CND Import"), dialogFactory, dialogService);
        add(cndImport);

        // cnd export
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public IDialogService.Dialog createDialog() {
                return new CndExportDialog();
            }
        };
        final DialogLink cndExport = new DialogLink("cnd-export", new Model<>("CND Export"), dialogFactory, dialogService);
        add(cndExport);

        // add namespace
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public IDialogService.Dialog createDialog() {
                return new NamespaceDialog();
            }
        };
        final DialogLink addNamespace = new DialogLink("add-namespace", new Model<>("Add Namespace"), dialogFactory, dialogService);
        add(addNamespace);

        // reset node
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public IDialogService.Dialog createDialog() {
                return new NodeResetDialog(getModel());
            }
        };
        final DialogLink resetNode = new DialogLink("reset-node", new Model<>("Reset Node"), dialogFactory, dialogService);
        add(resetNode);

        // create patch
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public IDialogService.Dialog createDialog() {
                return new CreatePatchDialog(config, getModel());
            }
        };
        final DialogLink createPatch = new DialogLink("create-patch", new Model<>("Create Patch"), dialogFactory, dialogService);
        add(createPatch);

        // apply patch
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public IDialogService.Dialog createDialog() {
                return new ApplyPatchDialog(getModel());
            }
        };
        final DialogLink applyPatch = new DialogLink("apply-patch", new Model<>("Apply Patch"), dialogFactory, dialogService);
        add(applyPatch);


        // View-permissions
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public IDialogService.Dialog createDialog() {
                return new PermissionsDialog(new NodeModelReference(LogoutPlugin.this, (JcrNodeModel) getDefaultModel()));
            }
        };
        final DialogLink permissions = new DialogLink("view-permissions", new Model<>("View Permissions"), dialogFactory, dialogService);
        add(permissions);

        // View-workflow
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;
            public IDialogService.Dialog createDialog() {
                return new WorkflowDialog(LogoutPlugin.this);
            }
        };
        final DialogLink workflow = new DialogLink("view-workflow", new Model<>("View Workflow"), dialogFactory, dialogService);
        add(workflow);

        // View References
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;
            public IDialogService.Dialog createDialog() {
                return new ReferencesDialog(LogoutPlugin.this);
            }
        };
        final DialogLink references = new DialogLink("view-references", new Model<>("View References"), dialogFactory, dialogService);
        add(references);

        // system info
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;
            public IDialogService.Dialog createDialog() {
                return new SystemInfoDialog();
            }
        };
        final DialogLink systemInfo = new DialogLink("system-info", new Model<>("Info"), dialogFactory, dialogService);
        add(systemInfo);

    }

}

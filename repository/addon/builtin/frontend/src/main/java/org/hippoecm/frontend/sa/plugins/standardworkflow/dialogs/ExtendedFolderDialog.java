// propertymodels are evil

/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.sa.plugins.standardworkflow.dialogs;

import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.sa.plugins.standardworkflow.PrototypeWorkflowPlugin;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.standardworkflow.PrototypeWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendedFolderDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;

    private transient static final Logger log = LoggerFactory.getLogger(PrototypeDialog.class);

    private Map<String, String> folderTypes;

    private String name;
    private String folderType;

    private DropDownChoice folderChoice;

    public ExtendedFolderDialog(PrototypeWorkflowPlugin plugin, IDialogService dialogWindow) {
        super(plugin, dialogWindow, "Add folder");

        name = "New folder";
        folderTypes = new TreeMap<String, String>();

        WorkflowsModel model = (WorkflowsModel) plugin.getModel();
        if (model.getNodeModel().getNode() == null) {
            ok.setEnabled(false);
        } else {
            try {
                QueryManager qmgr = model.getNodeModel().getNode().getSession().getWorkspace().getQueryManager();
                Query query = qmgr.createQuery("select * from hippo:templatetype", Query.SQL);
                QueryResult rs = query.execute();
                for (NodeIterator iter = rs.getNodes(); iter.hasNext();) {
                    Node prototypeNode = iter.nextNode();
                    if (prototypeNode.hasNode("hippo:prototype")) {
                        String documentType = prototypeNode.getName();
                        String prototypePath = prototypeNode.getNode("hippo:prototype").getPath();
                        if (!documentType.startsWith("hippo:") && !documentType.startsWith("reporting:")) {
                            /* FIXME: dropping the namespace has of course
                             * serious consequences, as when within different
                             * namespaces a same type is used, you can select
                             * only one.
                             */
                            if (documentType.contains(":"))
                                documentType = documentType.substring(documentType.indexOf(":") + 1);
                            folderTypes.put(documentType, prototypePath);
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error("could not obtain document types listing", ex);
            }
        }

        add(new TextFieldWidget("name", new PropertyModel(this, "name")));

        add(folderChoice = new DropDownChoice("type", new PropertyModel(this, "folderType"), new LinkedList(folderTypes
                .keySet())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean wantOnSelectionChangedNotifications() {
                return true;
            }
        });
        folderChoice.setNullValid(false);
        folderChoice.setRequired(true);
    }

    @Override
    protected void execute() throws Exception {
        PrototypeWorkflow workflow = (PrototypeWorkflow) getWorkflow();
        if (workflow != null) {
            String type = folderTypes.get(folderType);
            if (type == null) {
                log.error("unknown folder type " + folderType);
                return;
            }
            /* String path = */ workflow.addFolder(name, type);
        } else {
            log.error("no workflow defined on model for selected node");
        }
    }

    @Override
    public void cancel() {
    }
}

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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFolderWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    transient Logger log = LoggerFactory.getLogger(AbstractFolderWorkflowPlugin.class);

    protected static final String WORKFLOW_ACTION_LINK_ID = "workflow-action-dialog-link";
    protected static final String DIALOG_LINKS_COMPONENT_ID = "items";

    List<FolderWorkflowActionComponent> staticTemplates;
    LinkedList<FolderWorkflowActionComponent> templates;

    private Label folderName;

    public AbstractFolderWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        templates = new LinkedList<FolderWorkflowActionComponent>();
        add(folderName = new Label("foldername"));
        add(createDialogLinksComponent());
    }

    /**
     * Create the component that acts as a launcher for the workflow-action-components.
     * @return  new component that list the workflow-action-components
     */
    protected Component createDialogLinksComponent() {
        return new AbstractView(DIALOG_LINKS_COMPONENT_ID, getTemplateProvider()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item item) {
                final FolderWorkflowActionComponent wac = (FolderWorkflowActionComponent) item.getModel().getObject();
                item.add(createDialogActionComponent(wac));
            }
        };
    }

    /**
     * Create a new component that will trigger
     * @param component
     * @return
     */
    protected Component createDialogActionComponent(FolderWorkflowActionComponent component) {
        DialogLink link = new DialogLink(component.getId(), component.getLabel(), component.getAction());
        if (component.getIcon() != null) {
            link.add(new AttributeAppender("class", new Model(component.getIcon()), " "));
        }
        link.setEnabled(component.getAction().isEnabled());
        return link;
    }

    protected void addWorkflowAction(IModel label, String icon, Set<String> prototypes, DialogAction action) {
        getStaticTemplates().add(
                new FolderWorkflowActionComponent(WORKFLOW_ACTION_LINK_ID, label, icon, prototypes, action));
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LoggerFactory.getLogger(AbstractFolderWorkflowPlugin.class);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        WorkflowDescriptorModel model = (WorkflowDescriptorModel) AbstractFolderWorkflowPlugin.this.getModel();
        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
        try {
        if (model.getNode() != null) {
            folderName.setModel(new NodeTranslator(new JcrNodeModel(model.getNode())).getNodeName());
        }
            Workflow workflow = manager.getWorkflow((WorkflowDescriptor)model.getObject());
            if (workflow instanceof FolderWorkflow) {
                FolderWorkflow folderWorkflow = (FolderWorkflow) workflow;
                initializeTemplates(folderWorkflow.list());
            }
        } catch (MappingException ex) {
        } catch (WorkflowException ex) {
        } catch (RepositoryException ex) {
        } catch (RemoteException ex) {
        }
        redraw();
    }

    @Override
    public String getString(Map<String, String> criteria) {
        Map<String, String> map = new TreeMap<String, String>(criteria);
        map.put("hippo:workflow", FolderWorkflow.class.getName());
        return super.getString(map);
    }

    @SuppressWarnings("unchecked")
    public void select(JcrNodeModel nodeModel) {
        IBrowseService<JcrNodeModel> browser = getPluginContext().getService(
                getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
        IEditorManager editor = getPluginContext().getService(
                getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
        try {
            if (nodeModel.getNode() != null
                    && (nodeModel.getNode().isNodeType(HippoNodeType.NT_DOCUMENT) || nodeModel.getNode().isNodeType(
                            HippoNodeType.NT_HANDLE))) {
                if (browser != null) {
                    browser.browse(nodeModel);
                }
                if (!nodeModel.getNode().isNodeType("hippostd:folder")
                        && !nodeModel.getNode().isNodeType("hippostd:directory")) {
                    if (editor != null) {
                        JcrNodeModel editNodeModel = nodeModel;
                        Node editNodeModelNode = nodeModel.getNode();
                        if (editNodeModelNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                            editNodeModelNode = editNodeModelNode.getNode(editNodeModelNode.getName());
                        }
                        WorkflowManager workflowManager = ((UserSession) Session.get()).getWorkflowManager();
                        Workflow workflow = workflowManager.getWorkflow("editing", editNodeModelNode);
                        try {
                            if (workflow instanceof EditableWorkflow) {
                                EditableWorkflow editableWorkflow = (EditableWorkflow) workflow;
                                Document editableDocument = editableWorkflow.obtainEditableInstance();
                                if (editableDocument != null) {
                                    ((UserSession) Session.get()).getJcrSession().refresh(true);
                                    editNodeModel = new JcrNodeModel(((UserSession) Session.get()).getJcrSession()
                                            .getNodeByUUID(editableDocument.getIdentity()));
                                } else {
                                    editNodeModel = null;
                                }
                            }
                            if (editNodeModel != null) {
                                editor.openEditor(editNodeModel);
                            }
                        } catch (WorkflowException ex) {
                            log.error("Cannot auto-edit document", ex);
                        } catch (RemoteException ex) {
                            log.error("Cannot auto-edit document", ex);
                        } catch (RepositoryException ex) {
                            log.error("Cannot auto-edit document", ex);
                        } catch (ServiceException ex) {
                            log.error("Cannot auto-edit document", ex);
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }

    private void initializeTemplates(Map<String, Set<String>> list) {
        templates.clear();
        if (list != null && list.size() > 0) {
            for (Entry<String, Set<String>> entry : list.entrySet()) {
                templates.add(createWorkflowActionComponent(entry.getKey(), entry.getValue()));
            }
        }
        if (staticTemplates != null && staticTemplates.size() > 0) {
            for (FolderWorkflowActionComponent c : staticTemplates) {
                templates.add(c);
            }
        }
    }

    private List<FolderWorkflowActionComponent> getStaticTemplates() {
        if (staticTemplates == null) {
            staticTemplates = new LinkedList<FolderWorkflowActionComponent>();
        }
        return staticTemplates;
    }

    protected IDataProvider getTemplateProvider() {
        return new IDataProvider() {
            private static final long serialVersionUID = 1L;

            public IModel model(Object object) {
                return new Model((FolderWorkflowActionComponent) object);
            }

            public int size() {
                return templates.size();
            }

            public Iterator<FolderWorkflowActionComponent> iterator(int skip, int count) {
                return templates.iterator();
            }

            public void detach() {
            }
        };
    }

    protected FolderWorkflowActionComponent createWorkflowActionComponent(final String category,
            final Set<String> prototypes) {
        final IModel title = new StringResourceModel("add-category", (Component) this, null,
                new Object[] { new StringResourceModel(category, this, null) });

        DialogAction action = new DialogAction(new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {
                if (category.contains("New Smart Folder")) // FIXME very bad check on name
                    return new FolderWorkflowExtendedDialog(title, category, prototypes);
                else
                    return new FolderWorkflowDialog(title, category, prototypes);
            }
        }, getDialogService());

        // FIXME: proper procedure to get an icon
        String icon = null;
        if (category.toLowerCase().contains("folder")) {
            icon = "addfolder_ico";
        } else if (category.toLowerCase().contains("document")) {
            icon = "adddocument_ico";
        } else {
            icon = "addextended_ico";
        }
        return new FolderWorkflowActionComponent(WORKFLOW_ACTION_LINK_ID,
                new StringResourceModel(category, this, null), icon, prototypes, action);
    }

    public class FolderWorkflowActionComponent extends WorkflowActionComponent {
        private static final long serialVersionUID = 1L;

        private Set<String> prototypes;

        public FolderWorkflowActionComponent(String id, IModel label, String icon, Set<String> prototypes,
                DialogAction action) {
            super(id, label, icon, action);
            this.prototypes = prototypes != null ? prototypes : Collections.EMPTY_SET;
        }

        public Set<String> getPrototypes() {
            return prototypes;
        }
    }

public class FolderWorkflowDialog extends CompatibilityWorkflowPlugin.Dialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String category;
    private Set<String> prototypes;
    private String prototype;
    private String name;
    private IModel title;

    public FolderWorkflowDialog(IModel title, String category, Set<String> prototypes) {
        super();
        this.title = title;
        this.category = category;
        this.prototypes = prototypes;

        TextField text = new TextField("name", new PropertyModel(this, "name"));
        text.setRequired(true);
        add(text);

        Label typelabel = new Label("typelabel", new StringResourceModel("document-type", this, null));
        add(typelabel);

        if (prototypes.size() > 1) {

            final List<String> prototypesList = new LinkedList<String>(prototypes);
            DropDownChoice folderChoice;
            add(folderChoice = new DropDownChoice("prototype", new PropertyModel(this, "prototype"), prototypesList,
                    new TypeChoiceRenderer(this)));
            folderChoice.setNullValid(false);
            folderChoice.setRequired(true);

            // while not a prototype chosen, disable ok button
            Component notypes;
            add(notypes = new EmptyPanel("notypes"));
            notypes.setVisible(false);

        } else if (prototypes.size() == 1) {
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototype = prototypes.iterator().next();
            Component notypes;
            add(notypes = new EmptyPanel("notypes"));
            notypes.setVisible(false);
            typelabel.setVisible(false);
        } else {
            // if the folderWorkflowPlugin.templates.get(category).size() = 0 you cannot add this
            // category currently.
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototype = null;
            add(new Label("notypes", "There are no types available for : [" + category
                    + "] First create document types please."));
            text.setVisible(false);
            typelabel.setVisible(false);
        }
    }
    
    public IModel getTitle() {
        return title;
    }

    @Override
    protected String execute() {
        try {
        FolderWorkflow workflow = (FolderWorkflow) getWorkflow();
        if (prototype == null) {
            throw new IllegalArgumentException("You need to select a type");
        }
        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException("You need to enter a name");
        }
        if (workflow != null) {
            if (!prototypes.contains(prototype)) {
                log.error("unknown folder type " + prototype);
                return "Unknown folder type " + prototype;
            }
            String path = workflow.add(category, prototype, NodeNameCodec.encode(name, true));

            ((UserSession) Session.get()).getJcrSession().refresh(true);

            JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
            select(nodeModel);
        } else {
            log.error("no workflow defined on model for selected node");
        }
        } catch(Exception ex) {
            return ex.getClass().getName()+": "+ex.getMessage();
        }
        return null;
    }
}

public class FolderWorkflowExtendedDialog extends CompatibilityWorkflowPlugin.Dialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Set<String> prototypes;
    private String category;
    private String prototype;
    private String name;
    private String docbase;
    private String facet;
    private String value;
    private IModel title;

    public FolderWorkflowExtendedDialog(IModel title,
            String category, Set<String> prototypes) {
        super();
        this.title = title;
        this.category = category;
        this.prototypes = prototypes;

        add(new TextFieldWidget("name", new PropertyModel(this, "name")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                enableButtons();
            }
        });
        add(new TextFieldWidget("docbase", new PropertyModel(this, "docbase")));
        add(new TextFieldWidget("facet", new PropertyModel(this, "facet")));
        add(new TextFieldWidget("value", new PropertyModel(this, "value")));

        if (prototypes.size() > 1) {
            DropDownChoice folderChoice;
            add(folderChoice = new DropDownChoice("prototype", new PropertyModel(this, "prototype"),
                    new LinkedList<String>(prototypes)));
            folderChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onUpdate(AjaxRequestTarget target) {
                    enableButtons();
                }
            });

            folderChoice.setNullValid(false);
            folderChoice.setRequired(true);
        } else if (prototypes.size() == 1) {
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototype = prototypes.iterator().next();
        } else {
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototype = null;
        }

        enableButtons();
    }

    public IModel getTitle() {
        return title;
    }

    private void enableButtons() {
        try {
        WorkflowDescriptorModel model = (WorkflowDescriptorModel) getModel();
        ok.setEnabled(model.getNode() != null && prototype != null && name != null && !"".equals(name));
        }catch(RepositoryException ex) {
        }
    }

    @Override
    protected String execute() {
        try {
        FolderWorkflow workflow = (FolderWorkflow) getWorkflow();
        if (workflow != null) {
            if (!prototypes.contains(prototype)) {
                log.error("unknown folder type " + prototype);
                return "unknown folder type " + prototype;
            }
            Map<String, String> arguments = new TreeMap<String, String>();
            arguments.put("name", NodeNameCodec.encode(name, true));
            String path = (docbase.startsWith("/") ? docbase.substring(1) : docbase);
            arguments.put("docbase", ((UserSession) Session.get()).getJcrSession().getRootNode().getNode(path)
                    .getUUID());
            arguments.put("facet", facet);
            arguments.put("value", value);
            path = workflow.add(category, prototype, arguments);

            ((UserSession) Session.get()).getJcrSession().refresh(true);

            JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
            select(nodeModel);
        } else {
            log.error("no workflow defined on model for selected node");
        }
        return null;
        } catch(Exception ex) {
            return ex.getClass().getName()+": "+ex.getMessage();
        }
    }

}

}

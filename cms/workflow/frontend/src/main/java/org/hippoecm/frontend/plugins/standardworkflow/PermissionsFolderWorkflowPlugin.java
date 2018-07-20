/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.l10n.ResourceBundleModel;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionsFolderWorkflowPlugin extends RenderPlugin {

    private static final Logger log = LoggerFactory.getLogger(PermissionsFolderWorkflowPlugin.class);

    private static final String QUERY_LANGUAGE_QUERIES = Query.XPATH;
    private static final String QUERY_STATEMENT_QUERIES = "hippo:configuration/hippo:queries/hippo:templates//element(*, hippostd:templatequery)";
    private static final String HIPPO_TEMPLATES_BUNDLE_NAME = "hippo:templates";
    private static final IValueMap DIALOG_SIZE = new ValueMap("width=525,height=auto").makeImmutable();

    private String name;
    private String selected;
    private final List<DisplayModel> folderTypesList = new ArrayList<>();

    public PermissionsFolderWorkflowPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("queryModifier", new StringResourceModel("query-label", this), context, getModel()) {

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.inline(id, CmsIcon.FILE_UNLOCKED);
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                try {
                    Node folder = getModel().getNode();
                    PermissionsFolderWorkflowPlugin.this.name = ((HippoNode) folder).getDisplayName();
                    try {
                        Value[] values = folder.getProperty("hippostd:foldertype").getValues();
                        for (Value value : values) {
                            folderTypesList.add(new DisplayModel(value.getString()));
                        }
                    } catch (RepositoryException e) {
                        log.error("Couldn't get foldertypes from folder", e);
                    }
                } catch (RepositoryException ex) {
                    log.error("Couldn't get display name for folder", ex);
                    PermissionsFolderWorkflowPlugin.this.name = "";
                }
                Session session = UserSession.get().getJcrSession();
                Query query = null;

                try {
                    QueryManager qMgr = session.getWorkspace().getQueryManager();
                    query = qMgr.createQuery(QUERY_STATEMENT_QUERIES, QUERY_LANGUAGE_QUERIES);
                } catch (RepositoryException ex) {
                    log.error("Error retrieving all templatequeries: {}", ex);
                }

                final PropertyModel nameModel = new PropertyModel(PermissionsFolderWorkflowPlugin.this, "name");
                return new PermissionsConfirmDialog(getModel(), this, nameModel, query);
            }

            @Override
            protected void execute(WorkflowDescriptorModel model) throws Exception {
                Session session = UserSession.get().getJcrSession();
                Node folder = model.getNode();
                String[] store = new String[folderTypesList.size()];
                for (IModel propertyModel : folderTypesList) {
                    store = (String[]) ArrayUtils.add(store, propertyModel.getObject());
                }
                folder.setProperty("hippostd:foldertype", store);
                session.save();
            }
        });
    }

    @Override
    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) super.getModel();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSelected() {
        return selected;
    }

    public void setSelected(String selected) {
        this.selected = selected;
    }

    public class PermissionsConfirmDialog extends WorkflowDialog {

        protected static final String EXCLUDE = "exclude";

        @SuppressWarnings({"unchecked", "rawtypes"})
        public PermissionsConfirmDialog(WorkflowDescriptorModel model, IWorkflowInvoker invoker, IModel folderName, Query query) {
            super(invoker, model);

            setTitle(new StringResourceModel("title", this).setParameters(folderName.getObject()));
            setSize(DIALOG_SIZE);

            final IPluginConfig pluginConfig = getPluginConfig();
            List<String> excludes;
            if (pluginConfig.containsKey(EXCLUDE)) {
                excludes = Arrays.asList(pluginConfig.getStringArray(EXCLUDE));
            } else {
                excludes = Collections.emptyList();
            }

            //list of available queries:::
            final List<String> modelList = new ArrayList<>();
            try {
                if (query != null) {
                    QueryResult result = query.execute();
                    for (NodeIterator queryIterator = result.getNodes(); queryIterator.hasNext(); ) {
                        Node document = queryIterator.nextNode();
                        if (document != null) {
                            final String documentName = document.getName();
                            DisplayModel displayModel = new DisplayModel(documentName);
                            if (!folderTypesList.contains(displayModel) && !excludes.contains(documentName)) {
                                modelList.add(documentName);
                            }
                        }
                    }
                } else {
                    error("Error fetching existing foldertypes");
                }
            } catch (RepositoryException ex) {
                log.error("Error fetching existing foldertypes", ex);
                error("Error fetching existing foldertypes");
            }

            Collections.sort(modelList);

            final RefreshingView<DisplayModel> view = new RefreshingView<DisplayModel>("list-item-repeater") {

                @Override
                protected Iterator getItemModels() {
                    return folderTypesList.iterator();
                }

                @Override
                protected void populateItem(final Item item) {
                    final DisplayModel valueModel = (DisplayModel) item.getModel();

                    item.add(new Label("list-item-category", valueModel.getDisplayObject()));

                    Fragment fragment = new Fragment("action-fragment", "action-controls", PermissionsConfirmDialog.this);

                    WebMarkupContainer controls = new WebMarkupContainer("controls");
                    fragment.add(controls);

                    MarkupContainer remove = new AjaxLink("remove") {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            folderTypesList.remove(valueModel);
                            modelList.add(valueModel.getObject());
                            Collections.sort(modelList);
                            target.add(PermissionsConfirmDialog.this);
                        }
                    };
                    controls.add(remove);
                    final HippoIcon removeIcon = HippoIcon.fromSprite("remove-icon", Icon.TIMES);
                    remove.add(removeIcon);

                    MarkupContainer upLink = new AjaxLink("up") {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            final int i = folderTypesList.indexOf(valueModel);
                            Collections.swap(folderTypesList, i, i - 1);
                            target.add(PermissionsConfirmDialog.this);
                        }
                    };
                    boolean isFirst = (item.getIndex() == 0);
                    upLink.setEnabled(!isFirst);
                    controls.add(upLink);
                    final HippoIcon upIcon = HippoIcon.fromSprite("up-icon", Icon.ARROW_UP);
                    upLink.add(upIcon);

                    MarkupContainer downLink = new AjaxLink("down") {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            final int i = folderTypesList.indexOf(valueModel);
                            Collections.swap(folderTypesList, i, i + 1);
                            target.add(PermissionsConfirmDialog.this);
                        }
                    };
                    boolean isLast = (item.getIndex() == folderTypesList.size() - 1);
                    downLink.setEnabled(!isLast);
                    controls.add(downLink);
                    final HippoIcon downIcon = HippoIcon.fromSprite("down-icon", Icon.ARROW_DOWN);
                    downLink.add(downIcon);

                    item.add(fragment);
                    item.add(CssClass.append(
                        ReadOnlyModel.of(() -> (item.getIndex() & 1) == 1 ? "qfwli-even" : "qfwli-odd"))
                    );
                }
            };

            add(view);

            final StringResourceModel addTitleModel = new StringResourceModel("title-query-add", this)
                    .setParameters(folderName.getObject());
            Label addTitle = new Label("title-query-add", addTitleModel);
            add(addTitle);

            IChoiceRenderer<String> folderTypeRenderer = new IChoiceRenderer<String>() {
                public String getDisplayValue(final String object) {
                    final String categoryLabel = new StringResourceModel("add-category", PermissionsFolderWorkflowPlugin.this)
                            .setParameters(new ResourceBundleModel(HIPPO_TEMPLATES_BUNDLE_NAME, object))
                            .getString();
                    return String.format("%s (%s)", categoryLabel, object);
                }

                public String getIdValue(String object, int index) {
                    return object;
                }

                @Override
                public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
                    final List<? extends String> choices = choicesModel.getObject();
                    return choices.stream().filter(choice -> choice.equals(id)).findFirst().orElse(null);
                }
            };

            final DropDownChoice querySelection;

            add(querySelection = new DropDownChoice("query-selection", new PropertyModel(PermissionsFolderWorkflowPlugin.this, "selected"), modelList, folderTypeRenderer));
            querySelection.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (StringUtils.isNotEmpty(querySelection.getInput())) {
                        folderTypesList.add(new DisplayModel(querySelection.getInput()));
                        modelList.remove(querySelection.getInput());
                        target.add(PermissionsConfirmDialog.this);
                    }
                }
            });
            querySelection.setNullValid(true);
        }
    }

    private class DisplayModel extends Model<String> {

        public DisplayModel(String input) {
            super(input);
        }

        public String getDisplayObject() {
            final String categoryLabel = new StringResourceModel("add-category", PermissionsFolderWorkflowPlugin.this)
                    .setParameters(new ResourceBundleModel(HIPPO_TEMPLATES_BUNDLE_NAME, this.getObject()))
                    .getString();
            return String.format("%s (%s)", categoryLabel, this.getObject()); //categoryLabel;//
        }

    }

}

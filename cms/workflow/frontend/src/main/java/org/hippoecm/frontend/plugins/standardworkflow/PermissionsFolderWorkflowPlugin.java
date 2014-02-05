/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionsFolderWorkflowPlugin extends RenderPlugin {


    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PermissionsFolderWorkflowPlugin.class);

    private static final ResourceReference CSS = new CssResourceReference(PermissionsFolderWorkflowPlugin.class, "PermissionsFolderWorkflowPlugin.css");
    private static final String QUERY_LANGUAGE_QUERIES = Query.XPATH;
    private static final String QUERY_STATEMENT_QUERIES = "hippo:configuration/hippo:queries/hippo:templates//element(*, hippostd:templatequery)";

    private static final List<String> EMPTY = new ArrayList<String>();

    private String name;
    private String selected;
    private final List<DisplayModel> folderTypesList = new ArrayList<DisplayModel>();

    public PermissionsFolderWorkflowPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("queryModifier", new StringResourceModel("query-label", this, null), context, getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "queries-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                try {
                    Node folder = getModel().getNode();
                    PermissionsFolderWorkflowPlugin.this.name = ((HippoNode) folder).getLocalizedName();
                    try {
                        Value[] values = folder.getProperty("hippostd:foldertype").getValues();
                        for (Value value : values) {
                            folderTypesList.add(new DisplayModel(value.getString()));
                        }
                    } catch (RepositoryException e) {
                        log.error("Couldn't get foldertypes from folder", e);
                    }
                } catch (RepositoryException ex) {
                    log.error("Couldn't get localized name for folder", ex);
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

                return new PermissionsConfirmDialog(getModel(), this, new PropertyModel(PermissionsFolderWorkflowPlugin.this, "name"), query);
            }

            @Override
            protected void execute(WorkflowDescriptorModel model) throws Exception {
                Session session = ((UserSession) getSession()).getJcrSession();
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

    public class PermissionsConfirmDialog extends AbstractWorkflowDialog {
        private static final long serialVersionUID = 1L;
        protected static final String EXCLUDE = "exclude";
        private IModel folderName;


        @SuppressWarnings({"unchecked", "rawtypes"})
        public PermissionsConfirmDialog(WorkflowDescriptorModel model, IWorkflowInvoker invoker, IModel folderName, Query query) {
            super(model, invoker);

            this.folderName = folderName;

            final IPluginConfig pluginConfig = getPluginConfig();
            List<String> excludes;
            if (pluginConfig.containsKey(EXCLUDE)) {
                excludes = Arrays.asList(pluginConfig.getStringArray(EXCLUDE));
            } else {
                excludes = EMPTY;
            }

            //list of available queries:::
            final List<String> modelList = new ArrayList<String>();
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
                private static final long serialVersionUID = 1L;

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
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            folderTypesList.remove(valueModel);
                            modelList.add(valueModel.getObject());
                            Collections.sort(modelList);
                            target.add(PermissionsConfirmDialog.this);
                        }
                    };
                    controls.add(remove);

                    MarkupContainer upLink = new AjaxLink("up") {
                        private static final long serialVersionUID = 1L;

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

                    MarkupContainer downLink = new AjaxLink("down") {
                        private static final long serialVersionUID = 1L;

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

                    item.add(fragment);


                    item.add(new AttributeAppender("class", new AbstractReadOnlyModel() {
                        private static final long serialVersionUID = 1L;

                        public Object getObject() {
                            return ((item.getIndex() & 1) == 1) ? "qfwli-even" : "qfwli-odd";
                        }
                    }, " "));
                }
            };

            add(view);

            Label addTitle = new Label("title-query-add", new StringResourceModel("title-query-add", PermissionsConfirmDialog.this, null, folderName.getObject()));
            add(addTitle);

            IChoiceRenderer<String> folderTypeRenderer = new IChoiceRenderer<String>() {
                private static final long serialVersionUID = 1L;

                public String getDisplayValue(final String object) {
                    String categoryLabel = new StringResourceModel("add-category", PermissionsFolderWorkflowPlugin.this, null,
                            new StringResourceModel(object, PermissionsFolderWorkflowPlugin.this, null)).getString();
                    return String.format("%s (%s)", categoryLabel, object);//categoryLabel;
                }

                public String getIdValue(String object, int index) {
                    return object;
                }
            };


            final DropDownChoice querySelection;

            add(querySelection = new DropDownChoice("query-selection", new PropertyModel(PermissionsFolderWorkflowPlugin.this, "selected"), modelList, folderTypeRenderer));
            querySelection.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

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

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            response.render(CssHeaderItem.forReference(CSS));
        }

        private boolean isEditable(DisplayModel valueModel) {
            return false;
        }

        @Override
        public IModel getTitle() {
            return new StringResourceModel("title", this, null, folderName.getObject());
        }

        @Override
        public IValueMap getProperties() {
            return CUSTOM;
        }

        protected final IValueMap CUSTOM = new ValueMap("width=475,height=425").makeImmutable();
    }

    private class DisplayModel extends Model<String> {
        private static final long serialVersionUID = 1L;

        public DisplayModel(String input) {
            super(input);
        }

        public String getDisplayObject() {
            String categoryLabel = new StringResourceModel("add-category", PermissionsFolderWorkflowPlugin.this, null,
                    new StringResourceModel(this.getObject(), PermissionsFolderWorkflowPlugin.this, null)).getString();
            return String.format("%s (%s)", categoryLabel, this.getObject()); //categoryLabel;//
        }

    }

}

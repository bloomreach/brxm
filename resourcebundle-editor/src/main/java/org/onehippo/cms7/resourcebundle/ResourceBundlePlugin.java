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

package org.onehippo.cms7.resourcebundle;

import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.onehippo.cms7.resourcebundle.data.Bundle;
import org.onehippo.cms7.resourcebundle.data.Resource;
import org.onehippo.cms7.resourcebundle.data.ValueSet;
import org.onehippo.cms7.resourcebundle.dialogs.ResourceCopyDialog;
import org.onehippo.cms7.resourcebundle.dialogs.ResourceDeleteDialog;
import org.onehippo.cms7.resourcebundle.dialogs.ResourceEditDialog;
import org.onehippo.cms7.resourcebundle.dialogs.ResourceViewDialog;
import org.onehippo.cms7.resourcebundle.dialogs.ValueSetAddDialog;
import org.onehippo.cms7.resourcebundle.dialogs.ValueSetDeleteDialog;
import org.onehippo.cms7.resourcebundle.dialogs.ValueSetRenameDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders resource bundles multivalued properties
 *
 * @version "$Id: ResourceBundlePlugin.java 47 2014-06-26 12:34:25Z aschrijvers
 *          $"
 */
public class ResourceBundlePlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;
    public static final String MISSING_VALUE = "[<missing>]";

    private static Logger log = LoggerFactory.getLogger(ResourceBundlePlugin.class);

    public static final Pattern NAME_SPLITTER = Pattern.compile(":");

    private final IEditor.Mode mode;
    private final String keys;
    private final String defaultValueSetName;
    private Bundle bundle;
    private String selectedAnchor;
    private ValueSet selectedValueSet;
    private final IChoiceRenderer<ValueSet> valueSetRenderer;

    public ResourceBundlePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        keys = new StringResourceModel("plugin.display.keys.label", this, null).getObject();
        defaultValueSetName = new StringResourceModel("plugin.display.valueset.default", ResourceBundlePlugin.this, null).getObject();
        mode = IEditor.Mode.fromString(config.getString("mode", "view"));
        bundle = new Bundle(getModelObject(), defaultValueSetName);
        selectedAnchor = keys;
        selectedValueSet = bundle.getValueSets().get(0);

        // sharable choice renderer instance
        valueSetRenderer = new IChoiceRenderer<ValueSet>() {
            @Override
            public Object getDisplayValue(final ValueSet valueSet) {
                return valueSet.getDisplayName();
            }

            @Override
            public String getIdValue(final ValueSet valueSet, final int index) {
                return valueSet.getName();
            }

            @Override
            public ValueSet getObject(final String id, final IModel<? extends List<? extends ValueSet>> choicesModel) {
                final List<? extends ValueSet> choices = choicesModel.getObject();
                return choices.stream()
                        .filter(valueSet -> valueSet.getName().equals(id))
                        .findFirst()
                        .orElse(null);
            }
        };

        // display shows a table of resources, one resource is represented by one row in the table.
        final Component display = new WebMarkupContainer("display")
                .add(new ListView<Resource>("repeater", bundle.getResources()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(final ListItem<Resource> item) {
                        item.add(createResource(item.getModelObject()));
                    }
                })
                .setOutputMarkupId(true);
        add(display);

        add(new Label("enabled-label", new StringResourceModel("plugin.display.enabled.label", this, null))
                        .add(new Behavior() {
                            @Override
                            public void onComponentTag(final Component component, final ComponentTag tag) {
                                tag.put("title", new StringResourceModel("plugin.display.enabled.label.title",
                                        ResourceBundlePlugin.this, null).getObject());
                            }
                        })
        );
        add(new Label("key-label", keys));
        add(new Label("index-label", new StringResourceModel("plugin.display.index.label", this, null))
                        .add(new Behavior() {
                            @Override
                            public void onComponentTag(final Component component, final ComponentTag tag) {
                                tag.put("title", new StringResourceModel("plugin.display.index.label.title",
                                        ResourceBundlePlugin.this, null).getObject());
                            }
                        })
        );

        // add the value set selection drop-down
        add(new DropDownChoice<>("valueset-dropdown",
                new PropertyModel<ValueSet>(this, "selectedValueSet"),
                bundle.getValueSets(),
                valueSetRenderer)
                .setNullValid(false)
                .setRequired(true)
                .add(new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 1L;

                    protected void onUpdate(AjaxRequestTarget target) {
                        // Avoid that the anchor column shows the same content as the value columnn.
                        if (selectedValueSet.getDisplayName().equals(selectedAnchor)) {
                            if (selectedAnchor.equals(bundle.getDefaultValueSetName())) {
                                selectedAnchor = keys;
                            } else {
                                selectedAnchor = bundle.getDefaultValueSetName();
                            }
                        }

                        target.add(display);
                    }
                }));

        add(new WebMarkupContainer("value-set-editor")
                        .add(createValueSetAddLink())
                        .add(createValueSetRenameLink())
                        .add(createValueSetDeleteLink())
                        .setVisible(mode == IEditor.Mode.EDIT)
        );

        add(createResourceAddLink());
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(ResourceBundlePlugin.class, "resource-bundle-plugin.css")));
    }

    public IChoiceRenderer<ValueSet> getValueSetRenderer() {
        return valueSetRenderer;
    }

    protected WebMarkupContainer createResource(final Resource resource) {
        final String prefix = mode == IEditor.Mode.EDIT ? "edit" : "view";
        final WebMarkupContainer row = new Fragment("resource", prefix + "-resource", this, new CompoundPropertyModel<>(resource));
        final boolean hasDescription = StringUtils.isNotBlank(resource.getDescription());

        final IModel<Boolean> enabledModel = new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                return !MISSING_VALUE.equals(resource.getValue(selectedValueSet.getName()));
            }

            @Override
            public void setObject(final Boolean object) {
                if (!object && getObject()) {
                    resource.setValue(selectedValueSet.getName(), MISSING_VALUE);
                } else if (object && !getObject()) {
                    resource.setValue(selectedValueSet.getName(), "");
                }
            }

            @Override
            public void detach() {
            }
        };
        row.add(new AttributeAppender("class", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return enabledModel.getObject() ? "" : "disabled";
            }
        }));

        row.add(createEnableCheckBox(resource, enabledModel));
        row.add(new Image("desc", new PackageResourceReference(ResourceBundlePlugin.class, "description.png"))
                .add(new Behavior() {
                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("title", resource.getDescription());
                    }
                })
                .setVisible(hasDescription));

        Label key = new Label("anchor", new Model<String>() {
            @Override
            public String getObject() {
                Resource resource = (Resource) row.getDefaultModelObject();
                if (selectedAnchor.equals(keys)) {
                    return resource.getKey();
                }
                return resource.getValue(bundle.getNameFromDisplayName(selectedAnchor));
            }
        });
        if (hasDescription) {
            key.add(new Behavior() {
                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    tag.put("title", resource.getDescription());
                }
            });
        }
        row.add(key);
        row.add(new Label("value", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                Resource resource = (Resource) row.getDefaultModelObject();
                return resource.getValue(selectedValueSet.getName());
            }
        }) {
            @Override
            public boolean isVisible() {
                return enabledModel.getObject();
            }
        });
        row.add(new Label("index", Integer.toString(bundle.indexOfResource(resource))));
        if (mode == IEditor.Mode.EDIT) {
            row.add(createResourceEditLink(resource));
            row.add(createResourceCopyLink(resource));
            row.add(createResourceDeleteLink(resource));
        } else {
            row.add(createResourceViewLink(resource));
        }
        return row;
    }

    protected AjaxLink<Void> createResourceViewLink(final Resource resource) {
        AjaxLink<Void> link = new AjaxLink<Void>("action-view") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                getDialogService().show(new ResourceViewDialog(ResourceBundlePlugin.this, resource));
            }
        };
        link.setVisible(mode != IEditor.Mode.EDIT);
        link.add(makeTitle(this, "plugin.resource.action.view"));
        return link;
    }

    protected void refreshView() {
        redraw();
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.add(this);
        }
    }

    protected AjaxLink<Void> createResourceAddLink() {
        AjaxLink<Void> link = new AjaxLink<Void>("resource-add") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                getDialogService().show(new ResourceCopyDialog(ResourceBundlePlugin.this, bundle, null) {
                    @Override
                    protected void onOk() {
                        bundle.addResource(getResource()); // add the dialog's resource to the bundle
                        bundle.save();
                        refreshView();
                    }
                });
            }
        };
        link.setVisible(mode == IEditor.Mode.EDIT);
        link.add(makeTitle(this, "plugin.resource.action.add"));
        return link;
    }

    protected AjaxLink<Void> createResourceEditLink(final Resource resource) {
        AjaxLink<Void> link = new AjaxLink<Void>("action-edit") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                getDialogService().show(new ResourceEditDialog(ResourceBundlePlugin.this, resource) {
                    @Override
                    protected void onOk() {
                        bundle.save();
                        refreshView();
                    }
                });
            }
        };
        link.add(makeTitle(this, "plugin.resource.action.edit"));
        return link;
    }

    protected AjaxLink<Void> createResourceCopyLink(final Resource resource) {
        AjaxLink<Void> link = new AjaxLink<Void>("action-copy") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                getDialogService().show(new ResourceCopyDialog(ResourceBundlePlugin.this, bundle, resource) {
                    @Override
                    protected void onOk() {
                        bundle.addResource(getResource()); // add the dialog's resource to the bundle
                        bundle.save();
                        refreshView();
                    }
                });
            }
        };
        link.add(makeTitle(this, "plugin.resource.action.copy"));
        return link;
    }

    protected AjaxLink<Void> createResourceDeleteLink(final Resource resource) {
        AjaxLink<Void> link = new AjaxLink<Void>("action-delete") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                getDialogService().show(new ResourceDeleteDialog(ResourceBundlePlugin.this, resource) {
                    @Override
                    protected void onOk() {
                        bundle.deleteResource(resource);
                        bundle.save();
                        refreshView();
                    }
                });
            }
        };
        link.add(makeTitle(this, "plugin.resource.action.delete"));
        return link;
    }

    protected Component createEnableCheckBox(final Resource resource, final IModel<Boolean> enabledModel) {
        if (mode == IEditor.Mode.EDIT) {
            return new AjaxCheckBox("action-enabled", enabledModel) {

                @Override
                public void onUpdate(final AjaxRequestTarget target) {
                    bundle.save();
                    refreshView();
                }

            }.add(makeTitle(this, "plugin.resource.action.enable"));
        } else {
            return new Label("action-enabled", new LoadableDetachableModel<String>() {

                @Override
                protected String load() {
                    return enabledModel.getObject() ? "&#9745;" : "&#9744;";
                }

                @Override
                protected void onDetach() {
                    enabledModel.detach();
                }

            }).setEscapeModelStrings(false);
        }
    }

    protected AjaxLink<Void> createValueSetAddLink() {
        AjaxLink<Void> link = new AjaxLink<Void>("value-set-add") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                getDialogService().show(new ValueSetAddDialog(ResourceBundlePlugin.this, bundle) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onOk() {
                        bundle.addValueSet(getValueSet());
                        bundle.save();
                        refreshView();
                    }
                });
            }
        };
        link.add(makeTitle(this, "plugin.valueset.action.add"));
        return link;
    }

    protected AjaxLink<Void> createValueSetRenameLink() {
        AjaxLink<Void> link = new AjaxLink<Void>("value-set-rename") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                getDialogService().show(new ValueSetRenameDialog(ResourceBundlePlugin.this, bundle) {
                    @Override
                    protected void onOk() {
                        bundle.renameValueSet(getValueSet());
                        bundle.save();
                        refreshView();
                    }
                });
            }
        };
        link.setVisible(bundle.getMutableValueSets().size() > 0); // only show if there are mutable value sets.
        link.add(makeTitle(this, "plugin.valueset.action.rename"));
        return link;
    }

    protected AjaxLink<Void> createValueSetDeleteLink() {
        AjaxLink<Void> link = new AjaxLink<Void>("value-set-delete") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                getDialogService().show(new ValueSetDeleteDialog(ResourceBundlePlugin.this, bundle) {
                    @Override
                    protected void onOk() {
                        ValueSet valueSetToBeDeleted = getValueSet();
                        if (selectedValueSet == valueSetToBeDeleted) {
                            selectedValueSet = bundle.getValueSets().get(0);
                        }
                        bundle.deleteValueSet(valueSetToBeDeleted);
                        bundle.save();
                        refreshView();
                    }
                });
            }
        };
        link.setVisible(bundle.getMutableValueSets().size() > 0); // only show if there are mutable value sets.
        link.add(makeTitle(this, "plugin.valueset.action.delete"));
        return link;
    }

    public Behavior makeTitle(final Component base, final String key) {
        return new Behavior() {
            @Override
            public void onComponentTag(final Component component, final ComponentTag tag) {
                tag.put("title", new StringResourceModel(key, base, null).getObject());
            }
        };
    }

    @Override
    protected void onDetach() {
        bundle.detach();
        super.onDetach();
    }
}

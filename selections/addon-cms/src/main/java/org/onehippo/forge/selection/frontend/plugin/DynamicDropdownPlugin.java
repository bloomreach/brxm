/*
 * Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.frontend.plugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.Observer;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.TextDiffModel;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.model.ObservableValueModel;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.plugin.sorting.SortHelper;
import org.onehippo.forge.selection.frontend.provider.BasePathNameProvider;
import org.onehippo.forge.selection.frontend.provider.IValueListNameProvider;
import org.onehippo.forge.selection.frontend.provider.IValueListProvider;
import org.onehippo.forge.selection.frontend.utils.SelectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dynamic dropdown plugin, which is backed by a ValueListProvider service that provides a ValueList object.
 * <p/>
 * The plugin configuration can be provided with a <code>valuelist.provider</code> property, defaulting to
 * "service.valuelist.default", which is the name of the DocumentValueListProvider.
 * <p/>
 * The default DocumentValueListProvider reads a document of the type 'selection:valuelist', which contains key label
 * pairs used to display values and labels in the dropdown.
 * <p/>
 * The plugin configuration must then be provided with a <code>source</code> property, which can either be a valid UUID
 * of a handle or the path to the document based on the JCR root.
 * <p/>
 * List items that have a group name set, will be grouped together using OPTGROUP elements. Groups can be mixed with
 * regular option elements. Groups and options without a group are shown in  the order they are encountered in the
 * {@link org.onehippo.forge.selection.frontend.model.ValueList} used.
 */
public class DynamicDropdownPlugin extends RenderPlugin<String> {

    private static final String SERVICE_VALUELIST_DEFAULT = "service.valuelist.default";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(DynamicDropdownPlugin.class);
    private static final CssResourceReference CSS = new CssResourceReference(DynamicDropdownPlugin.class, "DynamicDropdownPlugin.css");
    private final SortHelper sortHelper = new SortHelper();

    @SuppressWarnings("unchecked")
    public DynamicDropdownPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        // check the cnd for being multiple, this is not supported (NB: check doesn't work for relaxed cnd)
        try {
            final JcrPropertyModel jcrPropertyModel = getValueModel().getJcrPropertymodel();
            if ((jcrPropertyModel != null) && (jcrPropertyModel.getProperty() != null)) {
                final boolean multiple = jcrPropertyModel.getProperty().getDefinition().isMultiple();
                if (multiple) {
                    throw new IllegalStateException(this.getClass().getName() + " does not support fields that are multiple: " +
                            "please use " + DynamicMultiSelectPlugin.class.getName() + " for that." +
                            " Field name is " + jcrPropertyModel.getProperty().getDefinition().getName() + ".");
                }
            } else {
                log.debug("JCR property model (or it's property) in {} is null while trying to determine if multiple. " +
                        "Config={}", this.getClass().getName(), config);
            }
        } catch (RepositoryException e) {
            throw new InstantiationError("Error instantiating " + this.getClass().getName() + ": " + e.getMessage());
        }

        // when this is an observable, register a model service with an observable value model
        final String observableId = config.getString(Config.OBSERVABLE_ID);
        if (StringUtils.isNotEmpty(observableId)) {
            final ModelReference<String> service = new ModelReference<>(observableId, new ObservableValueModel(getValueModel()));
            log.debug("Creating model service with observableId as name {}", observableId);
            service.init(context);
        }

        // when observing, track the registration of a model service by a dropdown configured as observable, with matching id
        final String observerId = config.getString(Config.OBSERVER_ID);
        if (StringUtils.isNotEmpty(observerId)) {

            context.registerTracker(new ServiceTracker<IModelReference>(IModelReference.class){

                private Observer<ObservableValueModel> observer;

                @Override
                protected void onServiceAdded(final IModelReference service, final String name) {

                    // match correct service name
                    if (!observerId.equals(name)) {
                        return;
                    }

                    log.debug("On model service added: matching service name and observerId {}", name);

                    // initialization; use existing value of the observable
                    final ObservableValueModel observedModel = (ObservableValueModel) service.getModel();
                    replaceControls(observedModel);

                    // lazy instantiation of the observer
                    if (observer == null) {
                        observer = new Observer<ObservableValueModel>(observedModel) {

                            @Override
                            public void onEvent(Iterator<? extends IEvent<ObservableValueModel>> events) {
                                // do not leave own value: the observable got changed so it's not valid anymore
                                getValueModel().setObject(null);

                                replaceControls(observer.getObservable());
                            }
                        };
                        context.registerService(observer, IObserver.class.getName());
                    }
                }

                @Override
                protected void onRemoveService(final IModelReference service, final String name) {
                    if (observer != null) {
                        context.unregisterService(observer, IObserver.class.getName());
                        observer = null;
                    }
                }
            }, observerId);
        }

        // value list name/id configured as source
        final ValueList configuredValueList = getValueList(config.getString(Config.SOURCE));

        final String mode = config.getString(ITemplateEngine.MODE);
        if ("edit".equals(mode)) {
            add(createSelect(configuredValueList));

            Label valueLabel = new Label("selectLabel");
            valueLabel.setVisibilityAllowed(false);
            add(valueLabel);
        } else {
            final DropDownChoice<?> dummyChoice = new DropDownChoice<String>("selectDropdown");
            dummyChoice.setVisibilityAllowed(false);
            add(dummyChoice);

            final Label label = createValueLabel(configuredValueList, mode);
            add(label);
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CSS));
    }

    protected void replaceControls(final ObservableValueModel observedValueModel) {

        final Object observedValue = observedValueModel.getObject();
        if (observedValue == null) {
            log.warn("Observed value from model {} is null", observedValueModel);
            return;
        }
        final String valueListName = getValueListName(observedValue);
        final ValueList valueList = getValueList(valueListName);

        final String mode = getPluginConfig().getString(ITemplateEngine.MODE);
        if ("edit".equals(mode)) {
            replace(createSelect(valueList));
            DynamicDropdownPlugin.this.redraw();
        }
        else {
            Label label = createValueLabel(valueList, mode);
            replace(label);
        }
    }

    /**
     * Get a value list name from a property value, using a name provider if configured.
     */
    protected String getValueListName(final Object objectValue) {

        if (!(objectValue instanceof String)) {
            throw new IllegalArgumentException("Argument observedValue is not a String but " +
                    ((objectValue == null) ? "null" : objectValue.getClass().getName()));
        }

        IValueListNameProvider valuelistNameProvider = null;

        // observedValue is value list name by default (no name provider)
        // (only String type supported)
        final String nameProvider = getPluginConfig().getString(Config.NAME_PROVIDER);
        if (StringUtils.isNotBlank(nameProvider)) {
            try {
                valuelistNameProvider = (IValueListNameProvider) Class.forName(nameProvider.trim()).newInstance();
            } catch (Exception e) {
                log.error("Cannot instantiate name provider class {}: {}. Continuing with default value list name provider",
                        new String[]{nameProvider, e.getMessage()});
            }
        }

        if (valuelistNameProvider == null) {
            log.debug("Using default value list name provider " + BasePathNameProvider.class.getSimpleName());
            valuelistNameProvider = new BasePathNameProvider();
        }

        final String valueListName = valuelistNameProvider.getValueListName((String) objectValue, getPluginConfig());
        if (StringUtils.isBlank(valueListName)) {
            log.warn("Provider {} could not provide value list name for value '{}'. Returning blank.", valuelistNameProvider.getClass().getName(), objectValue);
        }
        return valueListName;
    }

    /**
     * Create a Select Wicket component
     */
    protected Select createSelect(final ValueList valueList) {

        Select select = new Select("selectDropdown", getValueModel());
        if (valueList == null) {
            return select;
        }

        final IOptionRenderer<String> optionRenderer = new IOptionRenderer<String>() {
            private static final long serialVersionUID = 1L;

            public String getDisplayValue(String option) {
                return valueList.getListItemByKey(option).getLabel();
            }

            public IModel<String> getModel(String option) {
                return new Model<>(option);
            }
        };

        RepeatingView optionsListView = new RepeatingView("selectDropdownItem");

        int i = 0;
        final List<InternalSelectItem> selectItems = getInternalDropdownModel(valueList);
        for (InternalSelectItem selectItem : selectItems) {
            if (selectItem instanceof InternalSelectOptionGroup) {
                InternalSelectOptionGroup optionGroup = (InternalSelectOptionGroup) selectItem;
                Fragment optionGroupFragment = new OptionGroupFragment("dropdownListViewItem-" + i++,
                        "selectOptionGroup", optionGroup, optionRenderer);
                optionGroupFragment.setRenderBodyOnly(true);
                optionsListView.add(optionGroupFragment);
            } else {
                InternalSelectOption option = (InternalSelectOption) selectItem;
                OptionFragment optionFragment = new OptionFragment("dropdownListViewItem-" + i++, "selectOption",
                        option);
                optionFragment.setRenderBodyOnly(true);
                optionsListView.add(optionFragment);
            }
        }
        select.add(optionsListView);

        // add empty behavior so the model will get updated.
        select.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // no implementation necessary
            }
        });

        return select;
    }

    protected Label createValueLabel(final ValueList valueList, final String mode) {

        if (valueList == null) {
            return new Label("selectLabel", getValueModel());
        }

        Label label = null;
        if ("compare".equals(mode)) {
            if (getPluginConfig().containsKey("model.compareTo")) {
                IModelReference<String> baseRef =
                        getPluginContext().getService(getPluginConfig().getString("model.compareTo"), IModelReference.class);
                if (baseRef != null) {
                    IModel<String> baseModel = baseRef.getModel();
                    if (baseModel == null) {
                        log.info("base model service provides null model");
                        baseModel = new Model<>(null);
                    }
                    IModel<String> baseLabel = new ValueLabelModel(valueList, baseModel);
                    IModel<String> curLabel = new ValueLabelModel(valueList, getValueModel());
                    label = (Label) new Label("selectLabel",
                            new TextDiffModel(baseLabel, curLabel)).setEscapeModelStrings(false);
                } else {
                    log.warn("Opened in compare mode, but no base model service is available");
                }
            } else {
                log.warn("Opened in compare mode, but no base model was configured");
            }
        }
        if (label == null) {
            label = new Label("selectLabel", new ValueLabelModel(valueList, getModel()));
        }
        return label;
    }

    /**
     * Get a value list by name, using value list provider
     */
    protected ValueList getValueList(String valueListName) {
        // configured provider service, or the default
        String providerName = getPluginConfig().getString(Config.VALUELIST_PROVIDER, SERVICE_VALUELIST_DEFAULT);
        IValueListProvider selectedProvider = getPluginContext().getService(providerName, IValueListProvider.class);

        if (selectedProvider == null && !SERVICE_VALUELIST_DEFAULT.equals(providerName)) {
            selectedProvider = getPluginContext().getService(SERVICE_VALUELIST_DEFAULT, IValueListProvider.class);
        }

        if (selectedProvider == null) {
            log.warn("DynamicDropdownPlugin: value list provider can not be found by name '{}'",
                    getPluginConfig().getString(Config.VALUELIST_PROVIDER));

            DropDownChoice<String> dummyChoice = new DropDownChoice<>("selectDropdown");
            dummyChoice.setVisibilityAllowed(false);
            add(dummyChoice);
            Label valueLabel = new Label("selectLabel", new Model<>("-"));
            add(valueLabel);
            return null;
        }

        final Locale locale = SelectionUtils.getLocale(SelectionUtils.getNode(getModel()));
        final ValueList valueList = selectedProvider.getValueList(valueListName, locale);
        sortHelper.sort(valueList, getPluginConfig());

        return valueList;
    }

    private List<InternalSelectItem> getInternalDropdownModel(ValueList valueList) {
        List<InternalSelectItem> selectItems = new ArrayList<>(valueList.size());

        final Boolean showDefault = getPluginConfig().getAsBoolean(Config.SHOW_DEFAULT, true);
        if (showDefault) {
            selectItems.add(new InternalSelectOption("", new StringResourceModel("choose.one", this).getString()));
        }

        final Map<String, InternalSelectOptionGroup> groupsSoFar = new HashMap<>();
        for (ListItem listItem : valueList) {
            if (isGroupItem(listItem)) {
                InternalSelectOptionGroup groupForThisItem = groupsSoFar.get(listItem.getGroup());
                if (groupForThisItem == null) {
                    groupForThisItem = new InternalSelectOptionGroup(listItem.getGroup());
                    groupsSoFar.put(listItem.getGroup(), groupForThisItem);
                    selectItems.add(groupForThisItem);
                }
                groupForThisItem.add(listItem.getKey());
            } else {
                selectItems.add(new InternalSelectOption(listItem.getKey(), listItem.getLabel()));
            }
        }
        return selectItems;
    }

    private boolean isGroupItem(ListItem listItem) {
        return listItem.getGroup() != null;
    }

    protected JcrPropertyValueModel<String> getValueModel() {
        return (JcrPropertyValueModel<String>) getModel();
    }

    private abstract static class InternalSelectItem {
        private String label;

        public InternalSelectItem(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static class InternalSelectOption extends InternalSelectItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String value;

        public InternalSelectOption(String value, String label) {
            super(label);
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static class InternalSelectOptionGroup extends InternalSelectItem {
        ArrayList<String> options;

        public InternalSelectOptionGroup(String label) {
            super(label);
            this.options = new ArrayList<>();
        }

        public void add(String option) {
            options.add(option);
        }

        public List<String> getOptions() {
            return options;
        }
    }

    /**
     * Fragment containing a {@link org.apache.wicket.extensions.markup.html.form.select.Select} widget.
     */
    private class OptionGroupFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public OptionGroupFragment(String id, String markupId, InternalSelectOptionGroup optionGroup,
                                   IOptionRenderer<String> optionRenderer) {
            super(id, markupId, DynamicDropdownPlugin.this);
            // a container is bound to the optgroup element, so we can set  the label on the optgroup element
            MarkupContainer container = new WebMarkupContainer("optionGroup");

            final StringResourceModel labelModel = new StringResourceModel(optionGroup.getLabel(), this)
                    .setDefaultValue(optionGroup.getLabel());
            container.add(new AttributeModifier("label", labelModel));

            // the SelectOptions widget is a repeating view. This widget will thus replace the tag to which it is bound
            SelectOptions<String> optionGroupCmpt = new SelectOptions<>("optionGroupItems", optionGroup
                    .getOptions(), optionRenderer);
            container.add(optionGroupCmpt);

            add(container);
        }

    }

    /**
     * Fragment containing an {@link org.apache.wicket.extensions.markup.html.form.select.SelectOption}. The fragment is
     * a direct child of a select tag. We need this fragment to add a label to a {@link
     * org.apache.wicket.extensions.markup.html.form.select.SelectOption} widget.
     */
    private class OptionFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public OptionFragment(String id, String markupId, InternalSelectOption selectOption) {
            super(id, markupId, DynamicDropdownPlugin.this);
            SelectOption<String> wicketSelectOption = new SelectOption<String>("option", new Model<>(selectOption.getValue()));
            wicketSelectOption.add(new Label("optionLabel", selectOption.getLabel()));
            add(wicketSelectOption);
        }

    }
}

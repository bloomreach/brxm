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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.plugins.field.FieldPluginHelper;
import org.hippoecm.frontend.editor.plugins.fieldhint.FieldHint;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.properties.JcrMultiPropertyValueModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.LCS;
import org.hippoecm.frontend.plugins.standards.diff.LCS.Change;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.Violation;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.plugin.sorting.SortHelper;
import org.onehippo.forge.selection.frontend.provider.IValueListProvider;
import org.onehippo.forge.selection.frontend.utils.SelectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dynamic multiselect plugin, which is backed by a ValueListProvider service that provides a ValueList object.
 * <p/>
 * The default DocumentValueListProvider reads a document of the type 'selection:valuelist', which contains key label
 * pairs used to display values and labels in the dropdown.
 * <p/>
 * The plugin configuration must then be provided with a <code>source</code> property, which can either be a valid UUID
 * of a handle or the path to the document based on the JCR root.
 */
public class DynamicMultiSelectPlugin extends RenderPlugin {

    private final static String CONFIG_TYPE = "multiselect.type";
    private final static String CONFIG_SELECT_MAX_ROWS = "selectlist.maxrows";
    private final static String CONFIG_CHECKBOXES = "checkboxes";
    private final static String CONFIG_PALETTE = "palette";
    private final static String CONFIG_PALETTE_MAX_ROWS = "palette.maxrows";
    private final static String CONFIG_PALETTE_ALLOW_ORDER = "palette.alloworder";
    private final static String CONFIG_VALUELIST_OPTIONS = "valuelist.options";
    private final static String CONFIG_CLUSTER_OPTIONS = "cluster.options";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(DynamicMultiSelectPlugin.class);
    private static final CssResourceReference CSS = new CssResourceReference(DynamicMultiSelectPlugin.class, "DynamicMultiSelectPlugin.css");

    private final FieldPluginHelper helper;

    private JcrPropertyModel propertyModel;
    private IObserver propertyObserver;

    private final SortHelper sortHelper = new SortHelper();

    private IEditor.Mode mode;

    /**
     * Constructor.
     */
    public DynamicMultiSelectPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        this.mode = IEditor.Mode.fromString(config.getString(ITemplateEngine.MODE, "view"));

        helper = new FieldPluginHelper(context, config);

        subscribe();

        // use caption for backwards compatibility; i18n should use field name
        add(new Label("name", helper.getCaptionModel(this)));

        // required
        Label required = new Label("required", "*");
        if (helper.getField() != null && !helper.getField().getValidators().contains("required")) {
            required.setVisible(false);
        }
        add(required);

        add(new FieldHint("hint-panel", helper.getHintModel(this)));

        // configured provider
        final IValueListProvider selectedProvider = context.getService(config.getString(IValueListProvider.SERVICE),
                IValueListProvider.class);

        if (selectedProvider == null) {
            log.warn("DynamicMultiSelectPlugin: value list provider can not be found by name '{}'",
                    config.getString(IValueListProvider.SERVICE));

            // dummy markup
            final Fragment modeFragment = new Fragment("mode", "view", this);
            modeFragment.add(new ListView("viewitems", Collections.EMPTY_LIST, null));
            add(modeFragment);
            addListSelectionFragments(false/*visible*/, null, null);
            return;

        }

        final JcrMultiPropertyValueModel<String> model = new JcrMultiPropertyValueModel<>(getPropertyModel().getItemModel());

        //HIPPLUG-908: Start using cluster.options instead of valuelist.options, maintaining backwards compatibility.
        IPluginConfig options = config.getPluginConfig(CONFIG_CLUSTER_OPTIONS);
        if (options == null) {
            options = config.getPluginConfig(CONFIG_VALUELIST_OPTIONS);
            if (options == null) {
                throw new WicketRuntimeException("Configuration node '" + CONFIG_CLUSTER_OPTIONS
                        + "' not found in plugin configuration. " + config.toString());
            }

            log.warn("The configuration node name '{}' is deprecated. Rename it to '{}'. options={}",
                    new String[]{CONFIG_VALUELIST_OPTIONS, CONFIG_CLUSTER_OPTIONS, options.toString()});
        }

        final Locale locale = SelectionUtils.getLocale(SelectionUtils.getNode(model));
        final ValueList valueList = selectedProvider.getValueList(options.getString(Config.SOURCE), locale);

        sortHelper.sort(valueList, options);

        ArrayList<String> keys = new ArrayList<>(valueList.size());
        for (org.onehippo.forge.selection.frontend.model.ListItem item : valueList) {
            keys.add(item.getKey());
        }
        final IModel choicesModel = new Model<>(keys);

        Fragment modeFragment;
        final String mode = config.getString(ITemplateEngine.MODE);
        switch (mode) {
            case "edit":
                modeFragment = populateEditMode(config, model, valueList, choicesModel);
                break;
            case "compare":
                modeFragment = populateCompareMode(context, config, model, valueList);
                break;
            default:
                modeFragment = populateViewMode(model, valueList);
        }
        add(modeFragment);
    }

    @Override
    public void render(final PluginRequestTarget target) {

        if (isActive()) {
            if (IEditor.Mode.EDIT == mode) {
                IModel<IValidationResult> validationModel = helper.getValidationModel();
                if (validationModel != null && validationModel.getObject() != null) {
                    boolean valid = isFieldValid(validationModel.getObject());
                    if (!valid) {
                        target.appendJavaScript("Wicket.$('" + getMarkupId() + "').setAttribute('class', 'invalid');");
                    }
                }
            }
        }

        super.render(target);
    }

    protected FieldPluginHelper getFieldHelper() {
        return helper;
    }

    /**
     * Checks if a field has any violations attached to it.
     *
     * @param validation The IValidationResult that contains all violations that occurred for this editor
     * @return true if there are no violations present or non of the validation belong to the current field
     */
    protected boolean isFieldValid(final IValidationResult validation) {
        if (!validation.isValid()) {
            IFieldDescriptor field = getFieldHelper().getField();
            if (field == null) {
                return false;
            }
            for (Violation violation : validation.getViolations()) {
                Set<ModelPath> paths = violation.getDependentPaths();
                for (ModelPath path : paths) {
                    if (path.getElements().length > 0) {
                        ModelPathElement first = path.getElements()[0];
                        if (first.getField().equals(field)) {
                            return false;
                        }
                    }
                }
            }
        }
        IFeedbackMessageFilter filter = new ContainerFeedbackMessageFilter(this);
        return !getSession().getFeedbackMessages().hasMessage(filter);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CSS));
    }

    @Override
    protected void onDetach() {
        if (this.propertyModel != null) {
            this.propertyModel.detach();
        }
        super.onDetach();
    }

    @Override
    public void onModelChanged() {
        unsubscribe();
        subscribe();
    }

    protected Fragment populateViewMode(final JcrMultiPropertyValueModel<String> model, final ValueList valueList) {
        final Fragment modeFragment = new Fragment("mode", "view", this);// show view list
        modeFragment.add(new ListView("viewitems", model.getObject(), valueList));
        addListSelectionFragments(false/*visible*/, null, model);

        return modeFragment;
    }

    protected Fragment populateCompareMode(final IPluginContext context, final IPluginConfig config,
                                           final JcrMultiPropertyValueModel<String> model, final ValueList valueList) {
        final Fragment modeFragment = new Fragment("mode", "view", this);

        IModelReference compareToRef = context.getService(config.getString("model.compareTo"),
                IModelReference.class);
        if (compareToRef != null) {
            JcrNodeModel baseNodeModel = (JcrNodeModel)compareToRef.getModel();
            if (baseNodeModel != null && baseNodeModel.getNode() != null) {
                IFieldDescriptor field = helper.getField();
                try {
                    if (baseNodeModel.getNode().hasProperty(field.getPath())) {
                        JcrMultiPropertyValueModel<String> baseModel = new JcrMultiPropertyValueModel<>(
                                new JcrItemModel<>(baseNodeModel.getNode().getProperty(field.getPath()))
                        );

                        List<String> baseOptions = baseModel.getObject();
                        List<String> currentOptions = model.getObject();
                        List<Change<String>> changes = LCS.getChangeSet(baseOptions.toArray(new String[baseOptions.size()]),
                                currentOptions.toArray(new String[currentOptions.size()]));
                        // show view list
                        modeFragment.add(new CompareView("viewitems", changes, valueList));
                    } else {
                        modeFragment.add(new ListView("viewitems", model.getObject(), valueList));
                    }
                } catch (RepositoryException e) {
                    log.error("RepositoryException : ", e);
                }

            } else {
                modeFragment.add(new ListView("viewitems", model.getObject(), valueList));
            }
        } else {
            modeFragment.add(new ListView("viewitems", model.getObject(), valueList));
        }

        // hide dummy fragment
        addListSelectionFragments(false, null, model);
        return modeFragment;
    }

    protected Fragment populateEditMode(final IPluginConfig config, final JcrMultiPropertyValueModel<String> model,
                                        final ValueList valueList, final IModel choicesModel) {
        final Fragment modeFragment = new Fragment("mode", "edit", this);

        Fragment typeFragment;
        final String type = config.getString(CONFIG_TYPE);
        if (CONFIG_CHECKBOXES.equals(type)) {
            typeFragment = addCheckboxes(model, valueList, choicesModel);
        } else if (CONFIG_PALETTE.equals(type)) {
            typeFragment = addPalette(config, model, valueList, choicesModel);
        } else {
            typeFragment = addList(config, model, valueList, choicesModel);
        }
        modeFragment.add(typeFragment);
        return modeFragment;
    }

    protected Fragment addList(final IPluginConfig config, final JcrMultiPropertyValueModel<String> model,
                               final ValueList valueList, final IModel choicesModel) {
        final Fragment typeFragment = new Fragment("type", "edit-select", this);

        ListMultipleChoice multiselect = new ListMultipleChoice("multiselect", model, choicesModel,
                new ValueListItemRenderer(valueList));

        // trigger setObject on selection changed
        multiselect.add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });

        // set (configured) max rows
        final String maxRows = config.getString(CONFIG_SELECT_MAX_ROWS, "8");
        try {
            multiselect.setMaxRows(Integer.valueOf(maxRows));
        } catch (NumberFormatException nfe) {
            log.warn("The configured value '{}' for " + CONFIG_SELECT_MAX_ROWS + " is not a valid number. Defaulting to 8.", maxRows);
            multiselect.setMaxRows(8);
        }

        typeFragment.add(multiselect);
        addListSelectionFragments(true/*visible*/, multiselect, model);
        return typeFragment;
    }

    protected void addListSelectionFragments(boolean visible, ListMultipleChoice multiselect, JcrMultiPropertyValueModel<String> model) {
        final Fragment fragment = new Fragment("selectlinks", "edit-selectlinks", this);

        fragment.add(new SelectLink("select-link", multiselect, model));
        fragment.add(new UnselectLink("unselect-link", multiselect, model));
        fragment.setVisibilityAllowed(visible);

        add(fragment);
    }


    protected Fragment addPalette(final IPluginConfig config, final JcrMultiPropertyValueModel<String> model,
                                  final ValueList valueList, final IModel choicesModel) {
        final Fragment typeFragment = new Fragment("type", "edit-palette", this);

        // set (configured) max rows
        int rows = 10;
        final String maxRows = config.getString(CONFIG_PALETTE_MAX_ROWS, "10");
        try {
            rows = Integer.valueOf(maxRows);
        } catch (NumberFormatException nfe) {
            log.warn("The configured value '{}' for " + CONFIG_PALETTE_MAX_ROWS + " is not a valid number. Defaulting to 10.", maxRows);
        }

        // set (configured) allow order value
        final boolean allowOrder = config.getBoolean(CONFIG_PALETTE_ALLOW_ORDER);

        @SuppressWarnings("unchecked")
        final Palette palette = new Palette("palette", model, choicesModel,
                new ValueListItemRenderer(valueList), rows, allowOrder) {

            private static final long serialVersionUID = 1L;

            // FIXME: workaround for WICKET-2843
            @Override
            public Collection getModelCollection() {
                return new ArrayList(super.getModelCollection());
            }

            // trigger setObject on selection changed
            @Override
            protected Recorder newRecorderComponent() {
                Recorder recorder = super.newRecorderComponent();
                recorder.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                    }

                });
                return recorder;
            }
        };

        typeFragment.add(palette);

        // hide fragments for list view
        addListSelectionFragments(false/*visible*/, null, model);

        return typeFragment;
    }

    protected Fragment addCheckboxes(final JcrMultiPropertyValueModel<String> model, final ValueList valueList,
                                     final IModel choicesModel) {
        final Fragment typeFragment = new Fragment("type", "edit-checkboxes", this);

        CheckBoxMultipleChoice checkboxes = new CheckBoxMultipleChoice("checkboxes", model, choicesModel,
                new ValueListItemRenderer(valueList));

        // trigger setObject on selection changed
        checkboxes.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });

        typeFragment.add(checkboxes);

        // hide fragments for list view
        addListSelectionFragments(false/*visible*/, null, model);

        return typeFragment;
    }

    protected JcrPropertyModel getPropertyModel() {
        return new JcrPropertyModel(helper.getFieldItemModel());
    }

    /**
     * Subscribe to a service to get notified of property changes.
     */
    protected void subscribe() {
        propertyModel = getPropertyModel();
        if (propertyModel != null) {

            getPluginContext().registerService(propertyObserver = new IObserver() {

                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return propertyModel;
                }

                public void onEvent(Iterator events) {
                    redraw();
                }

            }, IObserver.class.getName());
        }
    }

    /**
     * Unsubscribe from the change notification service.
     */
    protected void unsubscribe() {
        if (propertyModel != null) {
            getPluginContext().unregisterService(propertyObserver, IObserver.class.getName());
            propertyModel = null;
        }
    }

    protected static class ValueListItemRenderer implements IChoiceRenderer<String> {

        private static final long serialVersionUID = 1L;

        private final ValueList valueList;

        public ValueListItemRenderer(final ValueList valueList) {
            this.valueList = valueList;
        }

        @Override
        public String getDisplayValue(String object) {
            return valueList.getLabel(object);
        }

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }

        @Override
        public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
            final List<? extends String> choices = choicesModel.getObject();
            return choices.contains(id) ? id : null;
        }
    }

    /**
     * Repeating view to show items in view mode.
     */
    protected class ListView extends RefreshingView<String> {

        private static final long serialVersionUID = 1L;
        private final Collection<IModel<String>> models = new ArrayList<>();

        public ListView(String id, Collection<?> actualValues, ValueList choices) {
            super(id);

            // get the choice labels by the actual values/keys
            for (Object item : actualValues) {
                this.models.add(new Model<String>(choices.getLabel(item)));
            }
        }

        @Override
        protected Iterator<IModel<String>> getItemModels() {
            return models.iterator();
        }

        @Override
        protected void populateItem(Item<String> item) {
            item.add(new Label("viewitem", item.getModelObject()));
        }
    }

    /**
     * Repeating view to show items in compare mode.
     */
    protected class CompareView extends org.apache.wicket.markup.html.list.ListView<Change<String>> {

        private static final long serialVersionUID = 1L;
        private final ValueList choices;

        public CompareView(String id, List<Change<String>> changes, ValueList choices) {
            super(id, changes);
            this.choices = choices;
        }

        @Override
        protected void populateItem(ListItem<Change<String>> item) {
            Change<String> change = item.getModelObject();

            Label label = new Label("viewitem", choices.getLabel(change.getValue()));
            switch (change.getType()) {
                case ADDED:
                    label.add(new AttributeAppender("class", new Model<>("hippo-diff-added"), " "));
                    break;
                case REMOVED:
                    label.add(new AttributeAppender("class", new Model<>("hippo-diff-removed"), " "));
                    break;
            }
            item.add(label);
        }
    }

    /**
     * TODO: UnselectLink and SelectLink should be combined and declared as static private classes.
     * It should be done in the next major release, i.e., 5.x.
     */

    /**
     * Link unselect all values from a select list.
     */
    protected class UnselectLink extends AjaxLink {

        private ListMultipleChoice multiselect;

        UnselectLink(String id, ListMultipleChoice multiselect, IModel model) {
            super(id, model);
            this.multiselect = multiselect;
        }

        @Override
        public void onClick(AjaxRequestTarget target) {

            // clear model
            setModelObject(null);

            // make the multiselect update to remove selected items
            target.add(this.multiselect);
        }
    }

    /**
     * Link select all values from a select list.
     */
    protected class SelectLink extends AjaxLink {

        private ListMultipleChoice multiselect;

        SelectLink(String id, ListMultipleChoice multiselect, IModel model) {
            super(id, model);
            this.multiselect = multiselect;
        }

        @Override
        public void onClick(AjaxRequestTarget target) {

            // select all options
            setModelObject(multiselect.getChoices());

            // make the multiselect update to remove selected items
            target.add(this.multiselect);
        }
    }
}

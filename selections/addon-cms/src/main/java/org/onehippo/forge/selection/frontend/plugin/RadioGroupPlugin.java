/*
 * Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.onehippo.forge.selection.frontend.plugin;

import java.util.Locale;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.l10n.ResourceBundleModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.utils.SelectionUtils;
import org.onehippo.forge.selection.frontend.wicket.RadioGroupWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_PROTOTYPESET;

/**
 * Radio group plugin.
 *
 * See {@link AbstractChoicePlugin} for general configuration details.
 *
 * The layout/orientation of the radio group items can be controlled by  setting
 * the configuration property "orientation" to either "horizontal" or "vertical":
 * this will order the items horizontally resp. vertically on the screen. The
 * default is horizontal orientation.
 *
 * @author Dennis Dam
 *
 */
public class RadioGroupPlugin extends AbstractChoicePlugin {
    private static final long serialVersionUID = 1L;

    /**
     * Name of the configuration parameter for orientation of the radio items.
     */
    public static final String CONFIG_ORIENTATION = "orientation";

    /**
     * Configuration value for horizontal orientation of radiogroup items
     */
    public static final String HORIZONTAL = "horizontal";

    /**
     * Configuration value for vertical orientation of radiogroup items
     */
    public static final String VERTICAL = "vertical";

    private static final Logger log = LoggerFactory.getLogger(RadioGroupPlugin.class);

    /**
     * Constructor for this class
     *
     * @param context plugin context
     * @param config plugin config
     */
    public RadioGroupPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (isInEditMode()) {
            addEditModeComponents();
        } else {
            IModel<?> base = null;
            if ("compare".equals(config.getString("mode", "view"))) {
                if (config.containsKey("model.compareTo")) {
                    IModelReference baseRef = context.getService(config.getString("model.compareTo"),
                            IModelReference.class);
                    if (baseRef != null) {
                        base = baseRef.getModel();
                    }
                }
            }
            if (base == null) {
                addReviewModeComponents();
            } else {
                addCompareModeComponents(base, getModel());
            }
        }
    }

    /**
     * Adds components that are shown when the plugin is in edit mode.
     */
    protected void addEditModeComponents() {
        Fragment fragment = new Fragment("fragment", "edit", this);
        add(fragment);
        addRadioGroupComponent(fragment);
    }

    /**
     * Adds a radiogroup component to a wicket container.
     * @param container the wicket container to which the radiogroup should be added
     */
    protected void addRadioGroupComponent(final MarkupContainer container) {
        ListItem selectedListItem = getSelectedValueItem();
        RadioGroupWidget<ListItem> radioGroup = new RadioGroupWidget<ListItem>("radioGroupWidget", getValueList(),
                Model.of(selectedListItem), isOrientationVertical()) {

            private static final long serialVersionUID = -6740997936511769980L;

            @Override
            protected void onChange(AjaxRequestTarget target, Object object) {
                ListItem listItem = (ListItem) object;
                if (listItem != null) {
                    RadioGroupPlugin.this.setSelectedItem(listItem);
                }
            }

            @Override
            protected IModel<String> getLabelModel(final String defaultLabel, final String propertyValue) {
                return RadioGroupPlugin.this.getLabelModel(defaultLabel, propertyValue);
            }
        };

        container.add(radioGroup);
    }

    /**
     * Returns whether radiogroup items should be shown vertically on the screen.
     *
     * @return true if orientation is vertical
     */
    protected boolean isOrientationVertical() {
        String orientation = getPluginConfig().getString(CONFIG_ORIENTATION);
        return VERTICAL.equals(orientation);
    }

    /**
     * Sets the key of the selected item as a value for the property value
     * associated with this plugin.
     * @param listItem the selected list item.
     */
    protected void setSelectedItem(ListItem listItem) {
        this.getValueModel().setObject(listItem.getKey());
    }

    /**
     * Returns the list item that is selected.
     * @return the selected item
     */
    protected ListItem getSelectedValueItem() {
        JcrPropertyValueModel valueModel = getValueModel();
        return getValueItem(valueModel);
    }

    protected ListItem getValueItem(IModel<?> model) {
        if(model == null || model.getObject() == null) {
            return null;
        }
        final String key = model.getObject().toString();
        return getValueList().getListItemByKey(key);
    }

    /**
     * Adds components that are shown when the plugin is in review mode.
     */
    protected void addReviewModeComponents() {
        Fragment fragment = new Fragment("fragment", "view", this);
        add(fragment);
        addSelectedValueLabel(fragment);
    }

    /**
     * Adds components that are shown when the plugin is in review mode.
     */
    protected void addCompareModeComponents(IModel<?> base, IModel<?> current) {
        Fragment fragment = new Fragment("fragment", "compare", this);

        ListItem currentListItem = getValueItem(current);
        String currentLabel = "";
        if (currentListItem != null) {
            currentLabel = currentListItem.getLabel();
        }
        fragment.add(new Label("new", getSelectedLabelModel(currentLabel))
                .add(new CssClassAppender(new Model<>("hippo-diff-added"))));

        ListItem baseListItem = getValueItem(base);
        String baseLabel = "";
        if (baseListItem != null) {
            baseLabel = baseListItem.getLabel();
        }
        fragment.add(new Label("old", getSelectedLabelModel(baseLabel))
                .add(new CssClassAppender(new Model<>("hippo-diff-removed"))));

        add(fragment);
    }

    /**
     * Adds a label showing the label of the currently selected value item to a
     * wicket container component.
     *
     * @param container a container component
     */
    protected void addSelectedValueLabel(final MarkupContainer container) {
        ListItem selectedListItem = getSelectedValueItem();
        String defaultLabel = "";
        if (selectedListItem != null) {
            defaultLabel = selectedListItem.getLabel();
        }
        Label valueLabel = new Label("selectLabel", getSelectedLabelModel(defaultLabel));
        container.add(valueLabel);
    }

    private IModel<String> getSelectedLabelModel(String defaultLabel) {
        return getLabelModel(defaultLabel, getSelectedValue());
    }

    private IModel<String> getLabelModel(final String defaultLabel, final String propertyValue) {
        final String bundleName = "hippo:types." + getDocumentType();
        final String key = getValueKey(propertyValue);
        final Locale locale = SelectionUtils.getLocale(SelectionUtils.getNode(getModel()));
        return new ResourceBundleModel.Builder(bundleName, key)
            .defaultValue(defaultLabel)
            .locale(locale)
            .build();
    }

    private String getDocumentType() {
        Node currentNode = (Node) ((JcrPropertyValueModel) getDefaultModel()).getJcrPropertymodel().getItemModel().getParentModel().getObject();
        try {
            while (!currentNode.isNodeType(NT_DOCUMENT) && !currentNode.isNodeType(NT_PROTOTYPESET)) {
                currentNode = currentNode.getParent();
            }
            if (currentNode.isNodeType(NT_DOCUMENT)) {
                return currentNode.getPrimaryNodeType().getName();
            }
            if (currentNode.isNodeType(NT_PROTOTYPESET)) {
                final Node templateType = currentNode.getParent();
                final Node namespace = templateType.getParent();
                return namespace.getName() + ":" + templateType.getName();
            }
        } catch (ItemNotFoundException e) {
            log.debug("Failed to determine containing document type of radio group", e);
        } catch (RepositoryException e) {
            log.warn("Failed to determing containing document type of radio group", e);
        }
        return "<unknown>";
    }

    private String getSelectedValue() {
        JcrPropertyValueModel propertyValueModel = (JcrPropertyValueModel) getDefaultModel();
        try {
            return propertyValueModel.getValue().getString();
        } catch (RepositoryException e) {
            log.warn("Failed to read property value");
        }
        return "<unknown>";
    }

    private String getValueKey(final String value) {
        JcrPropertyValueModel propertyValueModel = (JcrPropertyValueModel) getDefaultModel();
        JcrPropertyModel propertyModel = propertyValueModel.getJcrPropertymodel();
        try {
            return propertyModel.getProperty().getName() + "=" + value;
        } catch (RepositoryException e) {
            log.warn("Failed to determine property value translation key");
        }
        return "<unknown>";
    }
}

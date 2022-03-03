/*
 * Copyright 2010-2022 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.contentblocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.editor.prototype.JcrPrototypeStore;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.compare.IComparer;
import org.hippoecm.frontend.editor.editor.EditorForm;
import org.hippoecm.frontend.editor.editor.EditorPlugin;
import org.hippoecm.frontend.editor.plugins.field.AbstractFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.CollapsibleFieldTitle;
import org.hippoecm.frontend.editor.plugins.field.FieldPluginHelper;
import org.hippoecm.frontend.editor.plugins.field.FieldTitle;
import org.hippoecm.frontend.editor.plugins.field.FlagList;
import org.hippoecm.frontend.form.PostOnlyForm;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.ChildNodeProvider;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.AbstractRenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.Violation;
import org.hippoecm.frontend.validation.ViolationUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.forge.contentblocks.model.ContentBlockComparer;
import org.onehippo.forge.contentblocks.model.DropDownOption;
import org.onehippo.forge.contentblocks.sort.SortHelper;
import org.onehippo.forge.contentblocks.validator.ContentBlocksValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContentBlocksFieldPlugin provides authors with the ability to add different "content blocks" to a document with the
 * Document Editor.
 */
public class ContentBlocksFieldPlugin extends AbstractFieldPlugin<Node, JcrNodeModel> {

    private static final Logger log = LoggerFactory.getLogger(ContentBlocksFieldPlugin.class);

    public static final String LINKS = "links";
    public static final String DROPDOWN = "dropdown";

    private static final CssResourceReference CSS = new CssResourceReference(ContentBlocksFieldPlugin.class,
            "style.css");

    private static final int MAX_ITEMS_UNLIMITED = Integer.MAX_VALUE;
    private static final String MAX_ITEMS = "maxitems";
    private static final String CLUSTER_OPTIONS = "cluster.options";
    private static final String PROVIDER_COMPOUND = "cpItemsPath";
    private static final String COMPOUND_LIST = "compoundList";
    private static final String SHOW_COMPOUND_NAMES = "showCompoundNames";
    private static final String FIELD_CONTAINER_ID = "fieldContainer";
    private static final String CONTENTPICKER_ADD = "contentpicker-add";

    private final List<String> compoundList;
    private final String providerCompoundType;
    private final boolean showCompoundNames;
    private final int maxItems;
    private final FlagList collapsedItems = new FlagList();

    private Link<CharSequence> focusMarker;

    // each validator service id for a started clusters must be unique
    int validatorCount = 0;

    public ContentBlocksFieldPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final String configuredCompoundList = config.getString(COMPOUND_LIST);
        compoundList = compoundListFromConfiguration(configuredCompoundList);
        providerCompoundType = config.getString(PROVIDER_COMPOUND);
        if (configuredCompoundList == null && providerCompoundType == null) {
            log.error("Missing content picker configuration. Please make sure that the plugin configuration has " +
                    "either '{}' or '{}' set.", COMPOUND_LIST, PROVIDER_COMPOUND);
        }

        final IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig(CLUSTER_OPTIONS));
        maxItems = parameters.getInt(MAX_ITEMS, MAX_ITEMS_UNLIMITED);
        showCompoundNames = parameters.getAsBoolean(SHOW_COMPOUND_NAMES, false);

        final IModel<String> caption = helper.getCaptionModel(this);
        final IModel<String> hint = helper.getHintModel(this);
        final FieldTitle fieldTitle = new CollapsibleFieldTitle("field-title", caption, hint, helper.isRequired(), this);
        add(fieldTitle);

        final Component controls = createControls();
        controls.setVisible(isEditMode());
        add(controls);
    }

    private Component createControls() {
        final WebMarkupContainer controls = new WebMarkupContainer("blockControls");
        controls.add(createAddLinkLabel());

        final String type = getPluginConfig().getString("contentPickerType", LINKS);
        switch (type) {
            case LINKS:
                controls.add(new AddBlockWithLinks(CONTENTPICKER_ADD, this));
                break;
            case DROPDOWN:
                controls.add(new AddBlockWithDropDown(CONTENTPICKER_ADD, this));
                break;
            default:
                log.error("Invalid content picker type '{}'. Please make sure that property 'contentPickerType' in " +
                                "plugin config is either '{}' or '{}'. Falling back to '{}' type.",
                        type, LINKS, DROPDOWN, LINKS);
                controls.add(new AddBlockWithLinks(CONTENTPICKER_ADD, this));
                break;
        }

        controls.add(focusMarker = new FocusLink("focusMarker"));
        return controls;
    }

    @Override
    public void render(final PluginRequestTarget target) {
        if (isActive() && IEditor.Mode.EDIT == mode && target != null) {
            final String compoundSelector = String.format("#%s > .hippo-editor-compound-field > .hippo-editor-field",
                    getMarkupId());
            final String selector = String.format(
                    "$('%1$s > .hippo-editor-field-subfield').length " +
                            "? $('%1$s > .hippo-editor-field-subfield') " +
                            ": $('%1$s')",
                    compoundSelector);
            final FieldPluginHelper fieldHelper = getFieldHelper();
            final IFieldDescriptor field = fieldHelper.getField();
            final IModel<IValidationResult> validationModel = fieldHelper.getValidationModel();
            final String violationPerCompoundScript = ViolationUtils.getViolationPerCompoundScript(selector, field,
                    validationModel);
            if (violationPerCompoundScript != null) {
                target.appendJavaScript(violationPerCompoundScript);
            }
        }
        super.render(target);
    }

    /**
     * A field gets its own ValidationFilter, but a ContentBlocksField should not have that. By returning true here we
     * simulate that this plugin provides its own validation, and no ValidationFilter is added for it. The block itself
     * should not be validated. Its fields are validated by the {@link ContentBlocksValidator}.
     */
    @Override
    protected boolean doesTemplateSupportValidation() {
        return true;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
    }

    /**
     * Retrieves the prototype of the selected cpItem and adds it to the document node.
     *
     * @param cpItemTypeDescriptor type descriptor
     * @param path                 Target node to which the prototype needs to be added.
     */
    private void addCompoundType(final ITypeDescriptor cpItemTypeDescriptor, final String path) throws RepositoryException {
        if (log.isDebugEnabled()) {
            log.debug("copying {} prototype to {}", cpItemTypeDescriptor.getName(), path);
        }

        final JcrPrototypeStore jcrPrototypeStore = new JcrPrototypeStore();
        final JcrNodeModel prototype = jcrPrototypeStore.getPrototype(cpItemTypeDescriptor.getName(), false);
        final String destination = getModelObject().getPath() + "/" + path;

        final Session session = UserSession.get().getJcrSession();
        JcrUtils.copy(session, prototype.getNode().getPath(), destination);

        if (hasViolations()) {
            validateModelObjects();
        }
    }

    @Override
    protected IComparer<?> getComparer() {
        return new ContentBlockComparer(getTemplateEngine());
    }

    @Override
    protected ChildNodeProvider newProvider(final IFieldDescriptor descriptor, final ITypeDescriptor type, final IModel<Node> nodeModel) {
        try {
            final JcrNodeModel prototype = (JcrNodeModel) getTemplateEngine().getPrototype(type);
            return new ChildNodeProvider(descriptor, prototype, new JcrItemModel<>(nodeModel.getObject()));
        } catch (final TemplateEngineException ex) {
            log.warn("Could not find prototype", ex);
            return null;
        }
    }

    @Override
    protected boolean canAddItem() {
        return super.canAddItem() && getMaxItems() > 0 && getNumberOfItems() < getMaxItems();
    }

    @Override
    protected boolean canRemoveItem() {
        final IFieldDescriptor field = getFieldHelper().getField();
        if (IEditor.Mode.EDIT != mode || (field == null)) {
            return false;
        }

        // must be able to replace a first item with a content block of another type, so not this check
        // if (field.getValidators().contains("required") && provider.size() == 1) {
        //     return false;
        // }
        return field.isMultiple() || field.getValidators().contains("optional");
    }

    // the next five methods make these functions available to the ContentBlocksEditableFieldContainer class
    @Override
    protected boolean canReorderItems() {
        return super.canReorderItems();
    }

    @Override
    protected IPluginContext getPluginContext() {
        return super.getPluginContext();
    }

    @Override
    protected void redraw() {
        super.redraw();
    }

    @Override
    protected FieldPluginHelper getFieldHelper() {
        return super.getFieldHelper();
    }

    protected AbstractProvider<Node, JcrNodeModel> getProvider() {
        return provider;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public int getNumberOfItems() {
        try {
            final String itemPath = getFieldHelper().getField().getPath();
            return (int) getModelObject().getNodes(itemPath).getSize();
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
        return 0;
    }

    protected Component createAddLinkLabel() {
        final StringResourceModel nrOfItems = new StringResourceModel("nummItems", this, new Model<>(this));
        final Label label = new Label("addLabel", nrOfItems) {
            @Override
            public boolean isVisible() {
                // only show current and max items if max items is defined
                return maxItems != MAX_ITEMS_UNLIMITED;
            }
        };
        label.setOutputMarkupId(true);
        return label;
    }

    /**
     * Returns the template for the cpItem being added.
     *
     * @param id    The template id
     * @param mode  The mode this template is rendered in
     * @param model The backing model for this template
     * @return The template for the cpItem being added
     */
    @Override
    public IClusterControl newTemplate(final String id, IEditor.Mode mode, final IModel<?> model) throws TemplateEngineException {
        if (mode == null) {
            mode = this.mode;
        }
        log.debug("Locating template for {}", model);

        final ITemplateEngine engine = getTemplateEngine();
        final IClusterConfig template;
        if (model != null) {
            try {
                template = getTemplateEngine().getTemplate(engine.getType(model), mode);
            } catch (final TemplateEngineException ex) {
                if (IEditor.Mode.COMPARE == mode) {
                    throw new RuntimeException("Compare mode not supported for content block");
                } else {
                    log.warn("Could not find template for " + engine.getType(model).getName(), ex);
                    throw ex;
                }
            }
        } else {
            throw new RuntimeException("No model available (mode " + mode + ')');
        }

        if (log.isDebugEnabled()) {
            log.debug("Opening template for type " + engine.getType(model).getName());
        }

        final IPluginConfig parameters = new JavaPluginConfig(getPluginConfig().getPluginConfig(CLUSTER_OPTIONS));
        parameters.put(ITemplateEngine.ENGINE, getPluginConfig().getString(ITemplateEngine.ENGINE));
        parameters.put(AbstractRenderService.WICKET_ID, id);
        parameters.put(ITemplateEngine.MODE, mode.toString());
        parameters.put(IValidationService.VALIDATE_ID,
                getPluginContext().getReference(this).getServiceId() + ".validator." + (validatorCount++));

        return getPluginContext().newCluster(template, parameters);
    }

    @Override
    protected void populateEditItem(final Item<IRenderService> item, final JcrNodeModel model) {
        final boolean isCollapsed = collapsedItems.get(item.getIndex());
        item.add(new ContentBlocksEditableFieldContainer(FIELD_CONTAINER_ID,
                item, model, this, getBlockName(model), isCollapsed) {
            @Override
            protected void onCollapse(final boolean collapsed) {
                collapsedItems.set(item.getIndex(), collapsed);
            }
        });
    }

    @Override
    protected void populateViewItem(final Item<IRenderService> item, final JcrNodeModel model) {
        item.add(new ContentBlocksFieldContainer(FIELD_CONTAINER_ID, item, getBlockName(model), false));
    }

    @Override
    protected void populateCompareItem(final Item<IRenderService> item, final JcrNodeModel newModel, final JcrNodeModel oldModel) {
        populateViewItem(item, (newModel != null) ? newModel : oldModel);
    }

    private void addItem(final String type, final AjaxRequestTarget target) {
        try {
            final ITypeDescriptor selectedItemTypeDescriptor = getTemplateEngine().getType(type);
            if (selectedItemTypeDescriptor.isNode()) {
                addCompoundType(selectedItemTypeDescriptor, getFieldHelper().getField().getPath());
            } else {
                log.warn("Adding primitive types is not supported; type is {}", selectedItemTypeDescriptor.getName());
            }
            target.focusComponent(focusMarker);
            redraw();

        } catch (final TemplateEngineException | RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private boolean isEditMode() {
        return mode == IEditor.Mode.EDIT;
    }

    private List<String> compoundListFromConfiguration(final String configuredCompoundList) {

        // prepare document namespace
        String namespaceWithColon = "";
        try {
            final String documentType = getModelObject().getPrimaryNodeType().getName();
            if (!documentType.contains(":")) {
                log.warn("Cannot determine namespace from document type {} (no colon present)", documentType);
            }
            namespaceWithColon = documentType.substring(0, documentType.indexOf(":") + 1);
        } catch (final RepositoryException re) {
            log.error("Cannot determine namespace from document type", re);
        }

        // loop configured list and possibly prepend items with the document namespace
        final List<String> compoundList = new ArrayList<>();

        if (StringUtils.isNotBlank(configuredCompoundList)) {

            // split into list, removing commas and white spaces
            final String[] list = configuredCompoundList.split("\\s*,\\s*");
            for (final String compoundName : list) {
                if (!compoundName.contains(":")) {
                    compoundList.add(namespaceWithColon.concat(compoundName));
                } else {
                    compoundList.add(compoundName);
                }
            }
        }

        return compoundList;
    }

    /**
     * Get translated name of content block.
     */
    protected String getBlockName(final JcrNodeModel jcrNodeModel) {
        if (showCompoundNames && jcrNodeModel != null) {
            try {
                final String nodeType = jcrNodeModel.getNode().getPrimaryNodeType().getName();
                final IModel<String> compoundName = new TypeTranslator(new JcrNodeTypeModel(nodeType)).getTypeName();
                return compoundName.getObject();
            } catch (final RepositoryException e) {
                log.error("Cannot get compound block name", e);
            }
        }
        return StringUtils.EMPTY;
    }

    @Override
    public void onMoveItemUp(final JcrNodeModel model, final AjaxRequestTarget target) {
        super.onMoveItemUp(model, target);

        if (hasViolations()) {
            validateModelObjects();
        }
    }

    @Override
    public void onRemoveItem(final JcrNodeModel childModel, final AjaxRequestTarget target) {
        super.onRemoveItem(childModel, target);

        if (hasViolations()) {
            validateModelObjects();
        }
    }

    @Override
    public void onMoveItemToTop(final JcrNodeModel model) {
        super.onMoveItemToTop(model);

        if (hasViolations()) {
            validateModelObjects();
        }
    }

    @Override
    public void onMoveItemToBottom(final JcrNodeModel model) {
        super.onMoveItemToBottom(model);

        if (hasViolations()) {
            validateModelObjects();
        }
    }

    /**
     * If validation has already been done, trigger it again. This is useful when items in the form have moved to a
     * different location or have been removed. After redrawing a possible error message is shown at the correct field.
     */
    private void validateModelObjects() {
        final EditorPlugin editorPlugin = findParent(EditorPlugin.class);
        if (editorPlugin != null && editorPlugin.getForm() instanceof EditorForm) {
            final EditorForm editorForm = (EditorForm) editorPlugin.getForm();
            editorForm.onValidateModelObjects();
        }
    }

    protected void moveCollapsedItemToTop(final int index) {
        collapsedItems.moveTo(index, 0);
    }

    protected void moveCollapsedItemUp(final int index) {
        collapsedItems.moveUp(index);
    }

    protected void moveCollapsedItemDown(final int index) {
        collapsedItems.moveDown(index);
    }

    protected void moveCollapsedItemToBottom(final int index) {
        collapsedItems.moveTo(index, provider.size());
    }

    protected void removeCollapsedItem(final int index) {
        collapsedItems.remove(index);
    }

    private static class FocusLink extends Link<CharSequence> {

        private FocusLink(final String id) {
            super(id);
            setOutputMarkupId(true);
        }

        @Override
        protected CharSequence getURL() {
            return "#";
        }

        @Override
        public void onClick() {
            //do nothing
        }
    }

    private boolean hasViolations() {
        final IValidationResult result = helper.getValidationModel().getObject();
        final Set<Violation> violations = result.getViolations();

        return violations.stream()
                .anyMatch(this::hasMatchingField);
    }

    private boolean hasMatchingField(final Violation violation) {
        return violation.getDependentPaths().stream()
                .anyMatch(this::hasMatchingField);
    }

    private boolean hasMatchingField(final ModelPath modelPath) {
        return Stream.of(modelPath.getElements())
                .anyMatch(modelPathElement -> helper.getField().equals(modelPathElement.getField()));
    }

    /**
     * Add the list of content picker items as links
     */
    private class AddBlockWithLinks extends Fragment {

        public AddBlockWithLinks(final String id, final MarkupContainer container) {
            super(id, "addItemsLinks", container);
            setVisibilityAllowed(true);

            final List<String> compounds = compoundList.isEmpty() && providerCompoundType != null
                    ? getTypesFromFieldDescriptors()
                    : compoundList;

            final RepeatingView repeatingView = new RepeatingView("repeater");
            repeatingView.setVisibilityAllowed(true);

            for (final String compound : compounds) {
                // create markup item with link
                final WebMarkupContainer item = new WebMarkupContainer(repeatingView.newChildId());
                final AjaxLink<Void> link = new AjaxLink<Void>("addItem") {
                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        addItem(compound, target);
                    }
                };

                // add (translated) name of compound as link label
                final JcrNodeTypeModel nodeTypeModel = new JcrNodeTypeModel(compound);
                final IModel<String> compoundName = new TypeTranslator(nodeTypeModel).getTypeName();
                link.add(new Label("linkText", compoundName));
                link.add(HippoIcon.fromSprite("icon", Icon.PLUS));
                item.add(link);

                repeatingView.add(item);
            }

            add(repeatingView);
        }

        /**
         * Use the provider compound to maintain compatibility with the old configuration method
         *
         * @return A list of compound types
         */
        private List<String> getTypesFromFieldDescriptors() {
            try {
                final ITypeDescriptor compoundType = getTemplateEngine().getType(providerCompoundType);
                log.debug("The Content Blocks items are configured in {}", compoundType.getName());

                return getSortedFieldDescriptors(compoundType).stream()
                        .map(descriptor -> descriptor.getTypeDescriptor().getType())
                        .collect(Collectors.toList());
            } catch (final TemplateEngineException ex) {
                log.error(ex.getMessage(), ex);
            }
            return Collections.emptyList();
        }

        @Override
        public boolean isVisible() {
            return isEditMode() && canAddItem();
        }

    }

    private List<IFieldDescriptor> getSortedFieldDescriptors(final ITypeDescriptor compoundType) {
        final Map<String, IFieldDescriptor> fields = compoundType.getFields();
        final List<IFieldDescriptor> fieldDescriptors = new ArrayList<>(fields.values());
        final IPluginConfig config = getPluginConfig().containsKey(CLUSTER_OPTIONS)
                ? getPluginConfig().getPluginConfig(CLUSTER_OPTIONS)
                : getPluginConfig();

        final SortHelper helper = new SortHelper();
        helper.sort(fieldDescriptors, config);

        return fieldDescriptors;
    }

    /**
     * Adds the list of content picker items as dropdown
     */
    private class AddBlockWithDropDown extends Fragment {

        private DropDownOption selectedOption = null;

        public AddBlockWithDropDown(final String id, final MarkupContainer container) {
            super(id, "addItemsDropDown", container);
            setVisibilityAllowed(true);

            final Form<?> form = new PostOnlyForm<>("cpform");
            add(form);

            final List<DropDownOption> options = getOptions();
            // avoid first "Choose item" entry
            if (!options.isEmpty()) {
                selectedOption = options.get(0);
            }

            final DropDownChoice<DropDownOption> dropDown = new DropDownChoice<>("itemsDropDown",
                    new PropertyModel<>(this, "selectedOption"),
                    options,
                    new ChoiceRenderer<>("label", "value"));
            form.add(dropDown);

            final AjaxSubmitLink link = new AjaxSubmitLink("addItem", form) {
                @Override
                protected void onSubmit(final AjaxRequestTarget target) {
                    final String selectedValue = selectedOption != null ? selectedOption.getValue() : null;
                    log.debug("Selected value '{}' from dropdown", selectedValue);

                    if (StringUtils.isNotEmpty(selectedValue)) {
                        addItem(selectedValue, target);
                    }
                }
            };
            link.add(HippoIcon.fromSprite("icon", Icon.PLUS));
            link.add(new Label("linkText", ContentBlocksFieldPlugin.this.getString("addInputValue")));
            add(link);
        }

        @Override
        public boolean isVisible() {
            return isEditMode() && canAddItem();
        }

        private List<DropDownOption> getOptions() {
            return !compoundList.isEmpty() ? getOptionsFromList() :
                    providerCompoundType != null ? getOptionsFromProvider() : Collections.emptyList();
        }

        private List<DropDownOption> getOptionsFromProvider() {
            log.debug("Getting content picker items from compoundType {}", providerCompoundType);

            final List<DropDownOption> options = new ArrayList<>();

            try {
                final ITypeDescriptor cpItemTypeDescriptor = getTemplateEngine().getType(providerCompoundType);

                log.debug("The Content Blocks items are configured in {}", cpItemTypeDescriptor.getName());

                final Map<String, IFieldDescriptor> fields = cpItemTypeDescriptor.getFields();
                for (final IFieldDescriptor cpItemField : fields.values()) {

                    // value is actual JCR type, label is (translated) name of compound
                    final ITypeDescriptor typeDescriptor = cpItemField.getTypeDescriptor();
                    final String value = typeDescriptor.getName();
                    final JcrNodeTypeModel nodeTypeModel = new JcrNodeTypeModel(typeDescriptor.getType());
                    final String label = new TypeTranslator(nodeTypeModel).getTypeName().getObject();

                    options.add(new DropDownOption(value, label));
                }
            } catch (final TemplateEngineException ex) {
                log.error(ex.getMessage(), ex);
            }

            return options;
        }

        private List<DropDownOption> getOptionsFromList() {
            log.debug("Getting content picker items from compoundList {}", compoundList);

            final List<DropDownOption> options = new ArrayList<>();
            try {
                for (final String compound : compoundList) {
                    final String typeName = getTemplateEngine().getType(compound).getName();
                    final String label = new TypeTranslator(new JcrNodeTypeModel(typeName)).getTypeName().getObject();
                    options.add(new DropDownOption(compound, label));
                }
            } catch (final TemplateEngineException ex) {
                log.error(ex.getMessage(), ex);
            }

            return options;
        }
    }

}

/*
 * Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.compare.IComparer;
import org.hippoecm.frontend.editor.plugins.field.AbstractFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.FieldPluginHelper;
import org.hippoecm.frontend.editor.plugins.fieldhint.FieldHint;
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
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.forge.contentblocks.model.ContentBlockComparer;
import org.onehippo.forge.contentblocks.model.DropDownOption;
import org.onehippo.forge.contentblocks.sort.SortHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContentBlocksFieldPlugin provides authors with the ability to add different "content blocks" to a document with
 * the Document Editor.
 */
public class ContentBlocksFieldPlugin extends AbstractFieldPlugin<Node, JcrNodeModel> {

    static final Logger log = LoggerFactory.getLogger(ContentBlocksFieldPlugin.class);

    public static final String LINKS = "links";
    public static final String DROPDOWN = "dropdown";

    private static final CssResourceReference CSS = new CssResourceReference(ContentBlocksFieldPlugin.class, "style.css");

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

    private Link<CharSequence> focusMarker;

    // each validator service id for a started clusters must be unique
    int validatorCount = 0;

    public ContentBlocksFieldPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        FieldPluginHelper helper = new FieldPluginHelper(context, config);

        final String configuredCompoundList = config.getString(COMPOUND_LIST);
        compoundList = compoundListFromConfiguration(configuredCompoundList);
        providerCompoundType = config.getString(PROVIDER_COMPOUND);
        if ((configuredCompoundList == null) && (providerCompoundType == null)) {
            log.error("Missing content picker configuration. Please make sure that the plugin configuration has either " +
                    "'{}' or '{}' set.", COMPOUND_LIST, PROVIDER_COMPOUND);
        }

        IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig(CLUSTER_OPTIONS));
        maxItems = parameters.getInt(MAX_ITEMS, MAX_ITEMS_UNLIMITED);
        showCompoundNames = parameters.getAsBoolean(SHOW_COMPOUND_NAMES, false);

        // use caption for backwards compatibility; i18n should use field name

        add(new Label("name", helper.getCaptionModel(this)));

        IFieldDescriptor field = getFieldHelper().getField();
        Label required = new Label("required", "*");
        required.setVisible(field != null && (field.getValidators().contains("required")|| field.isMandatory()));
        add(required);

        add(new FieldHint("hint-panel", helper.getHintModel(this)));

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

    private void addCompoundType(ITypeDescriptor cpItemTypeDescriptor, String path) throws RepositoryException {
        if (log.isDebugEnabled()) {
            log.debug("copying {} prototype to {}", cpItemTypeDescriptor.getName(), path);
        }

        JcrPrototypeStore jcrPrototypeStore = new JcrPrototypeStore();
        JcrNodeModel prototype = jcrPrototypeStore.getPrototype(cpItemTypeDescriptor.getName(), false);
        String destination = getModelObject().getPath() + "/" + path;

        Session session = UserSession.get().getJcrSession();
        JcrUtils.copy(session, prototype.getNode().getPath(), destination);
    }

    @Override
    protected IComparer<?> getComparer() {
        return new ContentBlockComparer(getTemplateEngine());
    }

    @Override
    protected ChildNodeProvider newProvider(IFieldDescriptor descriptor, ITypeDescriptor type,
                                            IModel<Node> nodeModel) {
        try {
            JcrNodeModel prototype = (JcrNodeModel) getTemplateEngine().getPrototype(type);
            return new ChildNodeProvider(descriptor, prototype, new JcrItemModel<>(nodeModel.getObject()));
        } catch (TemplateEngineException ex) {
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
        if (!field.isMultiple() && !field.getValidators().contains("optional")) {
            return false;
        }
        // must be able to replace a first item with a content block of another type, so not this check
        // if (field.getValidators().contains("required") && provider.size() == 1) {
        //     return false;
        // }
        return true;
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

    protected AbstractProvider getProvider() {
        return provider;
    }
    
    public int getMaxItems() {
        return maxItems;
    }

    public int getNumberOfItems() {
        try {
            String itemPath = getFieldHelper().getField().getPath();
            return (int) getModelObject().getNodes(itemPath).getSize();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return 0;
    }

    protected Component createAddLinkLabel() {
        final StringResourceModel nrOfItems = new StringResourceModel("nummItems", this, new Model<>(this));
        Label label = new Label("addLabel", nrOfItems) {
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
     * @throws TemplateEngineException
     */
    @Override
    public IClusterControl newTemplate(String id, IEditor.Mode mode, IModel<?> model) throws TemplateEngineException {
        if (mode == null) {
            mode = this.mode;
        }
        log.debug("Locating template for {}", model);

        ITemplateEngine engine = getTemplateEngine();
        IClusterConfig template;
        if (model != null) {
            try {
                template = getTemplateEngine().getTemplate(engine.getType(model), mode);
            } catch (TemplateEngineException ex) {
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

        IPluginConfig parameters = new JavaPluginConfig(getPluginConfig().getPluginConfig(CLUSTER_OPTIONS));
        parameters.put(ITemplateEngine.ENGINE, getPluginConfig().getString(ITemplateEngine.ENGINE));
        parameters.put(AbstractRenderService.WICKET_ID, id);
        parameters.put(ITemplateEngine.MODE, mode.toString());
        parameters.put(IValidationService.VALIDATE_ID, getPluginContext().getReference(this).getServiceId() + ".validator." + (validatorCount++));

        return getPluginContext().newCluster(template, parameters);
    }

    @Override
    protected void populateEditItem(final Item<IRenderService> item, final JcrNodeModel model) {
        item.add(new ContentBlocksEditableFieldContainer(FIELD_CONTAINER_ID, item, model, this, getBlockName(model)));
   }

    @Override
    protected void populateViewItem(Item<IRenderService> item, final JcrNodeModel model) {
        item.add(new ContentBlocksFieldContainer(FIELD_CONTAINER_ID, item, getBlockName(model)));
    }

    @Override
    protected void populateCompareItem(Item<IRenderService> item, final JcrNodeModel newModel, final JcrNodeModel oldModel) {
        populateViewItem(item, newModel);
    }

    private void addItem(final String type, final AjaxRequestTarget target) {
        try {
            ITypeDescriptor selectedItemTypeDescriptor = getTemplateEngine().getType(type);
            if (selectedItemTypeDescriptor.isNode()) {
                addCompoundType(selectedItemTypeDescriptor, getFieldHelper().getField().getPath());
            } else {
                log.warn("Adding primitive types is not supported; type is {}", selectedItemTypeDescriptor.getName());
            }
            target.focusComponent(focusMarker);
            redraw();

        } catch (TemplateEngineException | RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private boolean isEditMode() {
        IEditor.Mode mode = IEditor.Mode.fromString(getPluginConfig().getString("mode"));
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
        } catch (RepositoryException re) {
            log.error("Cannot determine namespace from document type", re);
        }

        // loop configured list and possibly prepend items with the document namespace
        final List<String> compoundList = new ArrayList<>();

        if (StringUtils.isNotBlank(configuredCompoundList)) {

            // split into list, removing commas and white spaces
            final List<String> list = Arrays.asList(configuredCompoundList.split("\\s*,\\s*"));
            for (String compoundName : list) {
                if (!compoundName.contains(":")) {
                    compoundList.add(namespaceWithColon.concat(compoundName));
                }
                else {
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
            } catch (RepositoryException e) {
                log.error("Cannot get compound block name", e);
            }
        }
        return StringUtils.EMPTY;
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

    /**
     * Add the list of content picker items as links
     */
    private class AddBlockWithLinks extends Fragment {

        public AddBlockWithLinks(final String id, MarkupContainer container) {
            super(id, "addItemsLinks", container);
            setVisibilityAllowed(true);

            RepeatingView repeatingView = new RepeatingView("repeater");
            repeatingView.setVisibilityAllowed(true);

            try {
                // check if the compounds are configured as list
                if (!compoundList.isEmpty()) {

                    for (final String compound : compoundList) {

                        // create markup item with link
                        final WebMarkupContainer item = new WebMarkupContainer(repeatingView.newChildId());
                        final AjaxLink<Void> link = new AjaxLink<Void>("addItem") {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                addItem(compound, target);
                            }
                        };

                        // add (translated) name of compound as link label
                        final IModel<String> compoundName = new TypeTranslator(new JcrNodeTypeModel(compound)).getTypeName();
                        link.add(new Label("linkText", compoundName));
                        link.add(HippoIcon.fromSprite("icon", Icon.PLUS));
                        item.add(link);

                        repeatingView.add(item);
                    }

                    add(repeatingView);
                }
                // else use the provider compound to maintain compatibility with the previous method
                else if (providerCompoundType != null ) {
                    ITypeDescriptor cpItemTypeDescriptor = getTemplateEngine().getType(providerCompoundType);
                    log.debug("The Content Blocks items are configured in {}", cpItemTypeDescriptor.getName());
                    final Map<String, IFieldDescriptor> fields = cpItemTypeDescriptor.getFields();

                    List<IFieldDescriptor> fieldDescriptors = new ArrayList<>(fields.values());

                    final boolean options = getPluginConfig().containsKey(CLUSTER_OPTIONS);
                    SortHelper helper = new SortHelper();
                    helper.sort(fieldDescriptors, options ? getPluginConfig().getPluginConfig(CLUSTER_OPTIONS) : getPluginConfig());

                    for (final IFieldDescriptor fieldDesc : fieldDescriptors) {

                        // create markup item with link
                        final WebMarkupContainer item = new WebMarkupContainer(repeatingView.newChildId());
                        final AjaxLink<Void> link = new AjaxLink<Void>("addItem") {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                String type = fieldDesc.getTypeDescriptor().getType();
                                addItem(type, target);
                            }
                        };

                        // add (translated) name of compound as link label
                        final IModel<String> cpItemFieldName = new TypeTranslator(new JcrNodeTypeModel(fieldDesc.getTypeDescriptor().getType())).getTypeName();
                        link.add(new Label("linkText", cpItemFieldName));
                        link.add(HippoIcon.fromSprite("icon", Icon.PLUS));
                        item.add(link);

                        repeatingView.add(item);
                    }

                    add(repeatingView);
                }
            } catch (TemplateEngineException ex) {
                log.error(ex.getMessage(), ex);
            }
        }

        @Override
        public boolean isVisible() {
            return isEditMode() && canAddItem();
        }

    }

    /**
     * Adds the list of content picker items as dropdown
     */
    private class AddBlockWithDropDown extends Fragment {

        private DropDownOption selectedOption = null;

        public AddBlockWithDropDown(final String id, MarkupContainer container) {
            super(id, "addItemsDropDown", container);
            setVisibilityAllowed(true);

            final Form<?> form = new Form("cpform");
            add(form);

            final List<DropDownOption> options = getOptions();
            // avoid first "Choose item" entry
            if (options.size() > 0) {
                selectedOption = options.get(0);
            }

            final DropDownChoice dropDown = new DropDownChoice<>("itemsDropDown",
                    new PropertyModel<>(this, "selectedOption"),
                    options,
                    new ChoiceRenderer<>("label", "value"));
            form.add(dropDown);

            final AjaxSubmitLink link = new AjaxSubmitLink("addItem", form) {
                @Override
                protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                    final String selectedValue =  selectedOption != null ? selectedOption.getValue() : null;

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

        private List<DropDownOption> getOptionsFromProvider(){
            log.debug("Getting content picker items from compoundType {}", providerCompoundType);

            final List<DropDownOption> options = new ArrayList<>();

            try {
                ITypeDescriptor cpItemTypeDescriptor = getTemplateEngine().getType(providerCompoundType);

                log.debug("The Content Blocks items are configured in {}", cpItemTypeDescriptor.getName());

                Map<String, IFieldDescriptor> fields = cpItemTypeDescriptor.getFields();
                for (final IFieldDescriptor cpItemField: fields.values()) {

                    // value is actual JCR type, label is (translated) name of compound
                    final ITypeDescriptor typeDescriptor = cpItemField.getTypeDescriptor();
                    final String value = typeDescriptor.getName();
                    final String label = new TypeTranslator(new JcrNodeTypeModel(typeDescriptor.getType())).getTypeName().getObject();

                    options.add(new DropDownOption(value, label));
                }
            } catch (TemplateEngineException ex) {
                log.error(ex.getMessage(), ex);
            }

            return options;
        }

        private List<DropDownOption> getOptionsFromList() {
            log.debug("Getting content picker items from compoundList {}", compoundList);

            List<DropDownOption> options = new ArrayList<>();
            try {
                for (final String compound : compoundList) {
                    final String typeName = getTemplateEngine().getType(compound).getName();
                    final String label = new TypeTranslator(new JcrNodeTypeModel(typeName)).getTypeName().getObject();
                    options.add(new DropDownOption(compound, label));
                }
            } catch (TemplateEngineException ex) {
                log.error(ex.getMessage(), ex);
            }

            return options;
        }
    }

}

/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.richtext.dialog.images;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.editor.plugins.linkpicker.GalleryUploadPanel;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.richtext.dialog.AbstractBrowserDialog;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorImageLink;
import org.hippoecm.frontend.widgets.ThrottledTextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageBrowserDialog extends AbstractBrowserDialog<RichTextEditorImageLink> {

    private static final Logger log = LoggerFactory.getLogger(ImageBrowserDialog.class);

    private static final String CONFIG_PREFERRED_IMAGE_VARIANT = "preferred.image.variant";
    private static final String DEFAULT_PREFERRED_IMAGE_VARIANT = "hippogallery:original";
    private static final String EXCLUDED_IMAGE_VARIANTS = "excluded.image.variants";
    private static final String INCLUDED_IMAGE_VARIANTS = "included.image.variants";

    public static final List<String> ALIGN_OPTIONS =
            Collections.unmodifiableList(Arrays.asList("top", "middle", "bottom", "left", "right"));

    private boolean okSucceeded;
    DropDownChoice<String> type;

    private final Component uploadPanel;
    private final LinkedHashMap<String, String> nameTypeMap;
    private final IModel<RichTextEditorImageLink> imageModel;

    public ImageBrowserDialog(final IPluginContext context, final IPluginConfig config, final IModel<RichTextEditorImageLink> model) {
        super(context, config, model);

        setSize(DialogConstants.LARGE_AUTO);

        imageModel = model;

        add(uploadPanel = createUploadPanel());
        nameTypeMap = new LinkedHashMap<>();

        type = new DropDownChoice<>("type", new StringPropertyModel(model, RichTextEditorImageLink.TYPE),
                                    Collections.emptyList(), new IChoiceRenderer<String>() {

            @Override
            public Object getDisplayValue(final String object) {
                return nameTypeMap.get(object);
            }

            @Override
            public String getIdValue(final String object, final int index) {
                return object;
            }

        });
        type.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                checkState();
            }
        });
        type.setOutputMarkupId(true);

        // disable the type selector to prevent Wicket from initializing the drop down incorrectly
        // when a new image is uploaded (the form submit of the upload sets the model to null)
        type.setEnabled(false);

        add(type);

        add(new ThrottledTextFieldWidget("alt", new StringPropertyModel(model, RichTextEditorImageLink.ALT)) {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                checkState();
            }
        });

        final StringPropertyModel alignModel = new StringPropertyModel(model, RichTextEditorImageLink.ALIGN);
        final DropDownChoice<String> align = new DropDownChoice<>("align", alignModel,
                                                                  ALIGN_OPTIONS, new IChoiceRenderer<String>() {

            @Override
            public Object getDisplayValue(final String object) {
                return new StringResourceModel(object, ImageBrowserDialog.this, null).getString();
            }

            @Override
            public String getIdValue(final String object, final int index) {
                return object;
            }

        });
        align.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                checkState();
            }
        });

        align.setOutputMarkupId(true);
        align.setNullValid(false);
        add(align);

        initSelection();
        checkState();
    }

    private Component createUploadPanel() {
        final IPluginContext context = getPluginContext();
        final IPluginConfig config = getPluginConfig();
        final GalleryProcessor processor = DefaultGalleryProcessor.getGalleryProcessor(context, config);
        return new GalleryUploadPanel("upload-panel", new PropertyModel<>(this, "selectedFolderNode"),
                                      context, config, processor) {

            @Override
            protected void createGalleryItem(final FileUpload upload, final String galleryType) {
                super.createGalleryItem(upload, galleryType);

                // manually refresh feedback panel on an ajax request
                final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                if (target != null) {
                    target.add(feedback);
                }
            }
        };
    }

    /**
     * Return the selected node in the tree folder
     *
     * @return return the selected folder node
     */
    public Node getSelectedFolderNode() {
        final IModel<Node> folderModel = getFolderModel();
        if (folderModel != null) {
            return folderModel.getObject();
        } else {
            return null;
        }
    }

    @Override
    protected void onModelSelected(final IModel<Node> model) {
        type.setEnabled(true);

        setTypeChoices(model.getObject());

        if (type != null) {
            // if an preexisting image is selected, the constructor is not yet called so the class members are not initialized
            setPreferredTypeChoice();
            final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(type);
            }
        }
        super.onModelSelected(model);
    }

    /**
     * This is the callback to enable/disable the OK button of the image browser dialog. We abuse it here as a signal
     * that a new folder may have been selected. If so, refresh the upload-panel which contains the drop-down of
     * gallery types.
     *
     * @param isset pass-through parameter
     */
    @Override
    protected void setOkEnabled(final boolean isset) {
        super.setOkEnabled(isset);

        // function is called by parent constructor, when our constructor hasn't run yet...
        if (uploadPanel != null) {

            final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(uploadPanel);
            }
        }
    }

    private void setPreferredTypeChoice() {
        final IPluginConfig config = getPluginConfig();
        final String preferredType = config.getString(CONFIG_PREFERRED_IMAGE_VARIANT, DEFAULT_PREFERRED_IMAGE_VARIANT);
        if (nameTypeMap.containsKey(preferredType)) {
            imageModel.getObject().setType(preferredType);
        }
        if (StringUtils.isBlank(imageModel.getObject().getType())) {
            log.warn("Unknown preferred image variant: '{}'. Set the property '{}' to one of the available variants: {}",
                    preferredType, CONFIG_PREFERRED_IMAGE_VARIANT, nameTypeMap.keySet());
            if (!nameTypeMap.isEmpty()) {
                final String firstType = nameTypeMap.keySet().iterator().next();
                imageModel.getObject().setType(firstType);
            }
        }
    }

    private void setTypeChoices(final Node imageSetNode) {
        nameTypeMap.clear();

        final Set<Entry<String, String>> sortedEntries = new TreeSet<>(
                (o1, o2) -> {
                    final String s1 = o1.getValue();
                    final String s2 = o2.getValue();
                    return (s1 == null || s2 == null) ? -1 : s1.compareTo(s2);
                });

        try {
            Node tmpImageSetNode = imageSetNode;
            if (tmpImageSetNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                tmpImageSetNode = tmpImageSetNode.getNode(tmpImageSetNode.getName());
            }
            final TypeTranslator typeTranslator = new TypeTranslator(
                    new JcrNodeTypeModel(tmpImageSetNode.getPrimaryNodeType()));
            final NodeIterator childNodes = tmpImageSetNode.getNodes();
            final List<String> allImageVariants = getAllImageVariants(childNodes);
            final List<String> shownImageVariants = ShownImageVariantsBuilder.getAllowedList(allImageVariants,
                                                                                             getExcludedImageVariants(),
                                                                                             getIncludedImageVariants());
            for (final String childNodeName : shownImageVariants) {
                final IModel<String> propertyName = typeTranslator.getPropertyName(childNodeName);
                sortedEntries.add(new SimpleEntry<>(childNodeName, propertyName.getObject()));
            }
        } catch (final RepositoryException repositoryException) {
            log.error("Error updating the available image variants.", repositoryException);
        }

        for (final Entry<String, String> entry : sortedEntries) {
            nameTypeMap.put(entry.getKey(), entry.getValue());
        }

        if (type != null) {
            type.setChoices(new ArrayList<>(nameTypeMap.keySet()));
            type.updateModel();
        }
    }

    private List<String> getAllImageVariants(final NodeIterator childNodes) throws RepositoryException {
        final List<String> allImageVariants = new ArrayList<>();
        while (childNodes.hasNext()) {
            final Node childNode = childNodes.nextNode();
            if (childNode.isNodeType(HippoGalleryNodeType.IMAGE)) {
                final String childNodeName = childNode.getName();
                allImageVariants.add(childNodeName);
            }
        }
        return allImageVariants;
    }

    private List<String> getIncludedImageVariants() {
        return getMultipleString(INCLUDED_IMAGE_VARIANTS);
    }

    private List<String> getExcludedImageVariants() {
        return getMultipleString(EXCLUDED_IMAGE_VARIANTS);
    }

    private List<String> getMultipleString(final String key) {
        final List<String> result = new ArrayList<>();
        final IPluginConfig pluginConfig = getPluginConfig();
        if (!pluginConfig.containsKey(key)) {
            return null;
        }
        final String[] stringArray = pluginConfig.getStringArray(key);
        Collections.addAll(result, stringArray);
        return result;
    }

    @Override
    protected void onOk() {
        final RichTextEditorImageLink image = getModelObject();

        if (image.isValid()) {
            image.save();
            okSucceeded = true;
        } else {
            okSucceeded = false;
            error("Please select an image");
        }
    }

    @Override
    public void onClose() {
        if (!okSucceeded) {
            cancelled = true;
        }
        super.onClose();
    }

}

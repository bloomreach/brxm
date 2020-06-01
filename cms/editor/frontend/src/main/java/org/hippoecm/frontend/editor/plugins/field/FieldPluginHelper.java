/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for field plugins. It reads the field descriptor and produces a JcrItemModel.
 */
public class FieldPluginHelper implements IDetachable {

    private static final Logger log = LoggerFactory.getLogger(FieldPluginHelper.class);

    private static final String HIPPO_TYPES = "hippo:types";

    private final IPluginContext context;
    private final IPluginConfig config;

    private ITypeDescriptor documentType;
    private IFieldDescriptor field;
    private ValidationModel validationModel;

    public FieldPluginHelper(final IPluginContext context, final IPluginConfig config, final IFieldDescriptor field,
                             final ITypeDescriptor documentType, final ValidationModel validationModel) {
        this.context = context;
        this.config = config;
        this.field = field;
        this.documentType = documentType;
        this.validationModel = validationModel;
    }

    public FieldPluginHelper(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        final String typeName = config.getString(AbstractFieldPlugin.TYPE);
        final String fieldName = config.getString(AbstractFieldPlugin.FIELD);

        final ITemplateEngine engine = getTemplateEngine();
        if (engine == null) {
            log.error("No template engine available for type {}, field {}, config {}", typeName, fieldName, config);
            return;
        }

        try {
            if (typeName == null) {
                documentType = engine.getType(getNodeModel());
            } else {
                documentType = engine.getType(typeName);
            }

            if (documentType == null) {
                log.warn("No documentType found for type name {} or node model {}", typeName, getNodeModel());
            } else if (fieldName == null) {
                log.debug("No field was specified for type {} in the configuration {}", documentType.getName(), config);
            } else {
                field = documentType.getField(fieldName);
                if (field == null) {
                    log.warn("Could not find field with name {} in type {}; has the field been added " +
                            "without committing the document type?", fieldName, documentType.getName());
                } else if ("*".equals(field.getPath())) {
                    log.warn("Field path * is not supported, field name is {} in document type {}",
                            fieldName, documentType.getName());
                    field = null;
                } else if (field.getPath() == null) {
                    log.error("No path available for field {}", fieldName);
                    field = null;
                }
            }
        } catch (TemplateEngineException tee) {
            log.error("Could not resolve field for name " + fieldName, tee);
        }

        // FIXME: don't validate in "view" mode?
        if (config.containsKey(IValidationService.VALIDATE_ID)) {
            validationModel = new ValidationModel(context, config);
        } else {
            log.info("No validation service available for {}", fieldName);
        }
    }

    public ITypeDescriptor getDocumentType() {
        return documentType;
    }

    public IFieldDescriptor getField() {
        return field;
    }

    public IModel<IValidationResult> getValidationModel() {
        return validationModel;
    }

    public void detach() {
        if (field instanceof IDetachable) {
            ((IDetachable) field).detach();
        }
    }

    public String getFieldName() {
        return field != null ? field.getName() : "<unknown>";
    }

    protected IPluginConfig getPluginConfig() {
        return config;
    }

    protected IPluginContext getPluginContext() {
        return context;
    }

    public ITemplateEngine getTemplateEngine() {
        return context.getService(config.getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
    }

    public JcrNodeModel getNodeModel() {
        return (JcrNodeModel) context.getService(config.getString("wicket.model"),
                IModelReference.class).getModel();
    }

    public JcrItemModel getFieldItemModel() {
        return new JcrItemModel(getNodeModel().getItemModel().getPath() + "/" + field.getPath(),
                !field.getTypeDescriptor().isNode());
    }

    public IModel<String> getCaptionModel(final Component component) {
        return this.getCaptionModel(component, null);
    }

    public IModel<String> getCaptionModel(final Component component, final String defaultCaption) {

        String caption = (defaultCaption != null) ? defaultCaption : config.getString("caption");
        String captionKey;

        if (field != null) {
            captionKey = field.getPath();
            final String translation = getStringFromBundle(captionKey);
            if (translation != null) {
                return Model.of(translation);
            }

            captionKey = field.getName();
            if (caption == null && !captionKey.isEmpty()) {
                caption = StringUtils.capitalize(captionKey);
            }
        } else {
            captionKey = config.getString("captionKey");
            if (StringUtils.isNotEmpty(captionKey)) {
                final String translation = getStringFromBundle(captionKey);
                if (translation != null) {
                    return Model.of(translation);
                }
                if (caption == null) {
                    caption = StringUtils.capitalize(captionKey);
                }
            } else {
                captionKey = StringUtils.lowerCase(caption);
            }
        }

        // safety, may still occur
        if (captionKey == null) {
            return Model.of("undefined");
        }

        // this should be replaced by
        // return caption
        // after deprecation period of translator method
        // (deprecation started with CMS-9552)
        return new StringResourceModel(captionKey, component, null, caption);
    }

    public IModel<String> getHintModel(final Component component) {
        if (field == null) {
            return null;
        }
        final String key = field.getPath() + "#hint";
        final String translation = getStringFromBundle(key);
        if (translation != null) {
            return Model.of(translation);
        }
        final String hint = getPluginConfig().getString("hint");
        if (StringUtils.isNotBlank(hint)) {
            // this should be replaced by
            // return hint
            // after deprecation period of translator method
            // (deprecation started with CMS-9552)
            return new StringResourceModel(hint, component, null, hint);
        }
        return null;
    }

    String getStringFromBundle(final String key) {
        final String bundleName = getBundleName();
        if (bundleName != null) {
            final LocalizationService service = HippoServiceRegistry.getService(LocalizationService.class);
            if (service != null) {
                final ResourceBundle bundle = service.getResourceBundle(bundleName, Session.get().getLocale());
                if (bundle != null && bundle.getString(key) != null) {
                    return bundle.getString(key);
                }
            }
        }
        return null;
    }

    private String getBundleName() {
        return (documentType == null) ? null : HIPPO_TYPES + "." + documentType.getName();
    }
}

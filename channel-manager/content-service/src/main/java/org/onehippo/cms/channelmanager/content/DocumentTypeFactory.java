/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms.channelmanager.content.exception.DocumentTypeNotFoundException;
import org.onehippo.cms.channelmanager.content.model.DocumentTypeSpec;
import org.onehippo.cms.channelmanager.content.model.FieldTypeSpec;
import org.onehippo.cms.channelmanager.content.util.MockResponse;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DocumentTypeFactory assembles a DocumentTypeSpec for a document type ID, using primarily the
 * Repository's content type service.
 */
public class DocumentTypeFactory {
    private static final Logger log = LoggerFactory.getLogger(DocumentTypeFactory.class);
    private static final String HIPPO_TYPES = "hippo:types"; // TODO: this is defined in TranslatorType in the CMS API. Move to repository and avoid duplication?
    private static final String JCR_NAME = "jcr:name";       // TODO: this is defined in TranslatorType in the CMS API. Move to repository and avoid duplication?
    private static final String PROPERTY_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";
    private static final Map<String, FieldTypeSpec.Type> FIELD_TYPE_MAP;
    private static final Set<String> NAMESPACE_BLACKLIST;
    private static final Set<String> IGNORED_VALIDATORS;     // Known non-validating validator values
    private static final Map<String, FieldTypeSpec.Validator> VALIDATOR_MAP;
    private static final Set<String> FIELD_VALIDATOR_WHITELIST; // Unsupported validators of which we know they have field-scope only
    private static final Map<String, String> FIELD_PLUGIN_MAP;

    static {
        FIELD_TYPE_MAP = new HashMap<>();
        FIELD_TYPE_MAP.put("String", FieldTypeSpec.Type.STRING);
        FIELD_TYPE_MAP.put("Text",   FieldTypeSpec.Type.MULTILINE_STRING);

        NAMESPACE_BLACKLIST = new HashSet<>();
        NAMESPACE_BLACKLIST.add("hippo");
        NAMESPACE_BLACKLIST.add("hippostd");
        NAMESPACE_BLACKLIST.add("hippostdpubwf");
        NAMESPACE_BLACKLIST.add("hippotranslation");
        NAMESPACE_BLACKLIST.add("jcr");

        IGNORED_VALIDATORS = new HashSet<>();
        IGNORED_VALIDATORS.add("optional"); // optional "validator" indicates that the field may be absent (cardinality).

        VALIDATOR_MAP = new HashMap<>();
        VALIDATOR_MAP.put("required",  FieldTypeSpec.Validator.REQUIRED);
        VALIDATOR_MAP.put("non-empty", FieldTypeSpec.Validator.REQUIRED); // Apparently, making a String field required puts two values onto the validator property.

        FIELD_VALIDATOR_WHITELIST = new HashSet<>();
        FIELD_VALIDATOR_WHITELIST.add("email");
        FIELD_VALIDATOR_WHITELIST.add("escaped");
        FIELD_VALIDATOR_WHITELIST.add("html");
        FIELD_VALIDATOR_WHITELIST.add("image-references");
        FIELD_VALIDATOR_WHITELIST.add("references");
        FIELD_VALIDATOR_WHITELIST.add("required");
        FIELD_VALIDATOR_WHITELIST.add("resource-required");

        FIELD_PLUGIN_MAP = new HashMap<>();
        FIELD_PLUGIN_MAP.put("String", PROPERTY_FIELD_PLUGIN);
        FIELD_PLUGIN_MAP.put("Text",   PROPERTY_FIELD_PLUGIN);
    }

    private final Session systemSession; // Use read-only only!

    public DocumentTypeFactory(final Session systemSession) {
        this.systemSession = systemSession;
    }

    public DocumentTypeSpec getDocumentTypeSpec(final String id, final Locale locale) throws DocumentTypeNotFoundException {
        if ("ns:testdocument".equals(id)) {
            return MockResponse.createTestDocumentType();
        }

        final ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            final ContentType contentType = service.getContentTypes().getType(id);
            validateDocumentType(contentType, id);

            final DocumentTypeScanningContext context = createScanningContext(id, contentType, locale);
            final DocumentTypeSpec docType = new DocumentTypeSpec();

            docType.setId(id);
            docType.setDisplayName(determineDocumentDisplayName(id, context));
            populateFields(docType, context);

            return docType;
        } catch (RepositoryException e) {
            log.debug("Failed to create document type spect for '{}'.", id, e);
            throw new DocumentTypeNotFoundException();
        }
    }

    protected void validateDocumentType(final ContentType contentType, final String id) throws DocumentTypeNotFoundException {
        if (contentType == null) {
            log.debug("No content type found for '{}'", id);
            throw new DocumentTypeNotFoundException();
        }
        if (!contentType.isDocumentType()) {
            log.debug("Requested type '{}' is not document type", id);
            throw new DocumentTypeNotFoundException();
        }
    }

    protected void populateFields(final DocumentTypeSpec docType, final DocumentTypeScanningContext context) {
        context.contentType.getProperties()
                .entrySet()
                .stream()
                .filter(field -> isNotSystemProperty(field))
                .filter(field -> isSupportedFieldType(field))
                .filter(field -> usesDefaultFieldPlugin(field, context))
                .forEach(field -> addPropertyField(docType, field, context));
    }

    protected boolean isNotSystemProperty(final Map.Entry<String, ContentTypeProperty> field) {
        final String id = field.getKey();
        final String namespace = id.substring(0, id.indexOf(":"));

        return !NAMESPACE_BLACKLIST.contains(namespace);
    }

    protected boolean isSupportedFieldType(final Map.Entry<String, ContentTypeProperty> field) {
        final ContentTypeProperty property = field.getValue();

        return FIELD_TYPE_MAP.containsKey(property.getItemType());
    }

    protected boolean usesDefaultFieldPlugin(final Map.Entry<String, ContentTypeProperty> field,
                                             final DocumentTypeScanningContext context) {
        final ContentTypeProperty property = field.getValue();
        final Node fieldConfig = getConfigForField(context.documentTypeRoot, property.getName());
        if (fieldConfig != null) {
            try {
                final String plugin = fieldConfig.getProperty("plugin.class").getString();
                if (plugin.equals(FIELD_PLUGIN_MAP.get(property.getItemType()))) {
                    return true;
                }
            } catch (RepositoryException e) {
                // failed to read plugin config
            }
        }
        return false;
    }

    protected void addPropertyField(final DocumentTypeSpec docType,
                                  final Map.Entry<String, ContentTypeProperty> entry,
                                  final DocumentTypeScanningContext context) {
        final FieldTypeSpec fieldType = new FieldTypeSpec();
        final String fieldId = entry.getKey();
        final ContentTypeProperty property = entry.getValue();

        fieldType.setId(fieldId);
        fieldType.setDisplayName(determineFieldDisplayName(property, context));
        fieldType.setHint(determineFieldHint(property, context));
        fieldType.setType(FIELD_TYPE_MAP.get(property.getItemType()));

        if (property.isMultiple() || property.getValidators().contains("optional")) {
            fieldType.setMultiple(true);
        }

        for (String validator : property.getValidators()) {
            if (IGNORED_VALIDATORS.contains(validator)) {
                // Do nothing
            } else if (VALIDATOR_MAP.containsKey(validator)) {
                fieldType.addValidator(VALIDATOR_MAP.get(validator));
            } else if (FIELD_VALIDATOR_WHITELIST.contains(validator)) {
                fieldType.addValidator(FieldTypeSpec.Validator.UNSUPPORTED);
            } else {
                docType.setReadOnlyDueToUnknownValidator(true);
            }
        }

        docType.addField(fieldType);
    }

    protected String determineDocumentDisplayName(final String id, final DocumentTypeScanningContext context) {
        // Try to return a localised document name
        if (context.resourceBundle != null) {
            String displayName = context.resourceBundle.getString(JCR_NAME);
            if (displayName != null) {
                return displayName;
            }
        }
        // Fall-back to node name, use part after namespace prefix
        if (id.contains(":")) {
            String unNamespaced = id.substring(id.indexOf(":") + 1);
            return NodeNameCodec.decode(unNamespaced);
        }
        // No display name available
        return null;
    }

    protected String determineFieldDisplayName(final ContentTypeProperty property, final DocumentTypeScanningContext context) {
        // Try to return a localised field name
        if (context.resourceBundle != null) {
            String displayName = context.resourceBundle.getString(property.getName());
            if (displayName != null) {
                return displayName;
            }
        }
        // Try to read the caption property of the field's plugin config
        final Node fieldConfig = getConfigForField(context.documentTypeRoot, property.getName());
        if (fieldConfig != null) {
            try {
                return fieldConfig.getProperty("caption").getString();
            } catch (RepositoryException e) {
                // failed to read caption
            }
        }
        // No display name available
        return null;
    }

    protected String determineFieldHint(final ContentTypeProperty property, final DocumentTypeScanningContext context) {
        // Try to return a localised field name
        if (context.resourceBundle != null) {
            String hint = context.resourceBundle.getString(property.getName() + "#hint");
            if (hint != null) {
                return hint;
            }
        }
        // Try to read the caption property of the field's plugin config
        final Node fieldConfig = getConfigForField(context.documentTypeRoot, property.getName());
        if (fieldConfig != null) {
            try {
                return fieldConfig.getProperty("hint").getString();
            } catch (RepositoryException e) {
                // failed to read caption
            }
        }
        // No hint available
        return null;
    }

    protected Node getConfigForField(final Node documentTypeNode, final String fieldId) {
        try {
            final String nodeTypePath = HippoNodeType.HIPPOSYSEDIT_NODETYPE + "/" + HippoNodeType.HIPPOSYSEDIT_NODETYPE;
            final Node nodeTypeNode = documentTypeNode.getNode(nodeTypePath);
            final NodeIterator children = nodeTypeNode.getNodes();
            while (children.hasNext()) {
                final Node child = children.nextNode();
                if (child.hasProperty(HippoNodeType.HIPPO_PATH)
                        && child.getProperty(HippoNodeType.HIPPO_PATH).getString().equals(fieldId)) {
                    final String configPath = "editor:templates/_default_/" + child.getName();
                    return documentTypeNode.getNode(configPath);
                }
            }
        } catch (RepositoryException e) {
            // unable to retrieve the config node
        }
        return null;
    }

    protected DocumentTypeScanningContext createScanningContext(final String id, final ContentType contentType, final Locale locale)
            throws DocumentTypeNotFoundException {
        Node documentTypeRoot;

        try {
            String[] part = id.split(":");
            String path = "/hippo:namespaces/" + part[0] + "/" + part[1];

            documentTypeRoot = systemSession.getNode(path);
        } catch (RepositoryException e) {
            log.debug("Unable to find root node for document type '{}'", id, e);
            throw new DocumentTypeNotFoundException();
        }

        final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);
        final String bundleName = HIPPO_TYPES + "." + id;
        final ResourceBundle resourceBundle = localizationService.getResourceBundle(bundleName, locale);

        return new DocumentTypeScanningContext(contentType, resourceBundle, documentTypeRoot);
    }

    /**
     * DocumentTypeScanningContext reduces the need to pass long parameter lists of any of these resources through
     * nested method invocation.
     */
    private static class DocumentTypeScanningContext {
        public final ContentType contentType;
        public final ResourceBundle resourceBundle;
        public final Node documentTypeRoot;

        public DocumentTypeScanningContext(final ContentType contentType,
                                           final ResourceBundle resourceBundle,
                                           final Node documentTypeRoot) {
            this.contentType = contentType;
            this.resourceBundle = resourceBundle;
            this.documentTypeRoot = documentTypeRoot;
        }
    }
}

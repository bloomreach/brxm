/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.documenttype.field;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.jcr.Node;

import org.apache.commons.collections4.ListUtils;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.FieldScanningContext;
import org.onehippo.cms.channelmanager.content.documenttype.util.JcrBooleanReader;
import org.onehippo.cms.channelmanager.content.documenttype.util.JcrMultipleStringReader;
import org.onehippo.cms.channelmanager.content.documenttype.util.JcrStringReader;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms.services.validation.legacy.LegacyValidatorMapper;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;

/**
 * FieldTypeContext groups and wraps sources of information about a document type field.
 */
public class FieldTypeContext {

    private final String jcrName; // e.g. "myproject:date"
    private final String jcrType; // e.g. "Date"
    private final String type;    // e.g. "CalendarDate"
    private final boolean isProperty;
    private final boolean isMultiple;
    private final boolean isOrderable;
    private final List<String> validators;
    private final Node editorConfigNode;
    private final ContentTypeContext parentContext;

    /**
     * Create a FieldTypeContext given an editor configuration field node and a content type context.
     *
     * We only consider nodes that have a "field" property, and try to find the corresponding child node of the
     * content type's nodeType node (or of a supertype of the content type, to support inheritance).
     *
     * @param editorFieldConfigNode JCR node representing the field's editor configuration
     * @param context               content type context within which to create the field type context
     * @return                      derived FieldTypeContext or nothing, wrapped in an Optional
     */
    public static Optional<FieldTypeContext> create(final Node editorFieldConfigNode, final ContentTypeContext context) {
        return NamespaceUtils.getFieldProperty(editorFieldConfigNode)
                .flatMap(fieldName -> createForFieldName(fieldName, context, editorFieldConfigNode));
    }

    private static Optional<FieldTypeContext> createForFieldName(final String fieldName,
                                                                 final ContentTypeContext context,
                                                                 final Node editorFieldConfigNode) {
        return context.getFieldScanningContexts()
                .stream()
                .map(scanningContext -> createForParentType(scanningContext, fieldName, context, editorFieldConfigNode))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static FieldTypeContext createForParentType(final FieldScanningContext scanningContext,
                                                        final String fieldName,
                                                        final ContentTypeContext context,
                                                        final Node editorFieldConfigNode) {
        return NamespaceUtils.getPathForNodeTypeField(scanningContext.getNodeTypeNode(), fieldName)
                .map(path -> createForItem(path, scanningContext.getContentType(), context, editorFieldConfigNode))
                .orElse(null);
    }

    private static FieldTypeContext createForItem(final String itemName,
                                                  final ContentType parentType,
                                                  final ContentTypeContext context,
                                                  final Node editorFieldConfigNode) {
        final ContentTypeItem item = parentType.getItem(itemName);
        return item != null ? new FieldTypeContext(item, context, editorFieldConfigNode) : null;
    }

    private static List<String> getValidators(final ContentTypeItem contentTypeItem) {
        // Validators defined on the contentType have a higher priority than those defined on the field.
        final List<String> contentTypeItemValidators = contentTypeItem.getValidators();
        return getValidatorsForType(contentTypeItem)
                .map(contentTypeValidators -> ListUtils.union(contentTypeValidators, contentTypeItemValidators))
                .orElse(contentTypeItemValidators);
    }

    private static Optional<List<String>> getValidatorsForType(final ContentTypeItem contentTypeItem) {
        final String jcrType = contentTypeItem.getEffectiveType();
        return ContentTypeContext.getContentType(jcrType)
                .flatMap(contentType -> contentType.isCompoundType()
                        ? Optional.of(contentType.getValidators())
                        : Optional.empty());
    }

    private FieldTypeContext(final ContentTypeItem contentTypeItem,
                             final ContentTypeContext parentContext,
                             final Node editorConfigNode) {
        this(contentTypeItem.getName(), contentTypeItem.getEffectiveType(), contentTypeItem.getItemType(),
                contentTypeItem.isProperty(), contentTypeItem.isMultiple(), contentTypeItem.isOrdered(),
                getValidators(contentTypeItem), parentContext, editorConfigNode);
    }

    public FieldTypeContext(final ContentTypeItem contentTypeItem, final ContentTypeContext parentContext) {
        this(contentTypeItem, parentContext, null);
    }

    public FieldTypeContext(final String jcrName,
                            final String jcrType,
                            final String type,
                            final boolean isProperty,
                            final boolean isMultiple,
                            final boolean isOrderable,
                            final List<String> validators,
                            final ContentTypeContext parentContext,
                            final Node editorConfigNode) {
        this.jcrName = jcrName;
        this.jcrType = jcrType;
        this.type = type;
        this.isProperty = isProperty;
        this.isMultiple = isMultiple;
        this.isOrderable = isOrderable;
        this.validators = LegacyValidatorMapper.legacyMapper(validators, type);
        this.parentContext = parentContext;
        this.editorConfigNode = editorConfigNode;
    }

    public String getJcrName() {
        return jcrName;
    }

    public String getType() {
        return type;
    }

    public String getJcrType() {
        return jcrType;
    }

    public boolean isProperty() {
        return isProperty;
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public boolean isOrderable() {
        return isOrderable;
    }

    public List<String> getValidators() {
        return validators;
    }

    public ContentTypeContext getParentContext() {
        return parentContext;
    }

    public Optional<Node> getEditorConfigNode() {
        return Optional.ofNullable(editorConfigNode);
    }

    public Optional<ContentTypeContext> createContextForCompound() {
        return ContentTypeContext.createFromParent(type, getParentContext());
    }

    public Optional<Boolean> getBooleanConfig(final String propertyName) {
        return NamespaceUtils.getConfigProperty(this, propertyName, JcrBooleanReader.get());
    }

    public Optional<String> getStringConfig(final String propertyName) {
        return NamespaceUtils.getConfigProperty(this, propertyName, JcrStringReader.get());
    }

    public Optional<String[]> getMultipleStringConfig(final String propertyName) {
        return NamespaceUtils.getConfigProperty(this, propertyName, JcrMultipleStringReader.get());
    }
}

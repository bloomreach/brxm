/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.forge.selection.frontend.plugin.Config;

public class DynamicDropdownFieldType extends PropertyFieldType {

    private boolean showDefault = true;
    private String source = null;
    private String sortComparator = null;
    private String sortBy = null;
    private String sortOrder = null;
    private String valueListProvider = null;
    private boolean observation = false;

    public DynamicDropdownFieldType() {
        setType(Type.DYNAMIC_DROPDOWN);
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        fieldContext.getStringConfig(Config.SHOW_DEFAULT).ifPresent(this::setShowDefault);
        fieldContext.getStringConfig(Config.SOURCE).ifPresent(this::setSource);
        fieldContext.getStringConfig(Config.SORT_COMPARATOR).ifPresent(this::setSortComparator);
        fieldContext.getStringConfig(Config.SORT_BY).ifPresent(this::setSortBy);
        fieldContext.getStringConfig(Config.SORT_ORDER).ifPresent(this::setSortOrder);
        fieldContext.getStringConfig(Config.VALUELIST_PROVIDER).ifPresent(this::setValueListProvider);
        // configuration properties for field observation:
        fieldContext.getStringConfig(Config.NAME_PROVIDER).ifPresent(s -> this.setObservation());
        fieldContext.getStringConfig(Config.OBSERVABLE_ID).ifPresent(s -> this.setObservation());
        fieldContext.getStringConfig(Config.OBSERVER_ID).ifPresent(s -> this.setObservation());
        fieldContext.getStringConfig(Config.SOURCE_BASE_PATH).ifPresent(s -> this.setObservation());
        return super.init(fieldContext);
    }

    @Override
    public boolean isSupported() {
        return isDefaultValueListProvider() && !isObservation() && super.isSupported();
    }

    private boolean isDefaultValueListProvider() {
        return StringUtils.isBlank(valueListProvider) || valueListProvider.equals("service.valuelist.default");
    }

    @Override
    protected String getDefault() {
        return StringUtils.EMPTY;
    }

    @Override
    public Object getValidatedValue(final FieldValue value, final CompoundContext context) {
        return value.getValue();
    }

    /**
     * Call this method to signal that the dropdown is configured to serve as observer or observed field. This is not
     * supported yet in the Visual Editor.
     */
    private void setObservation() {
        observation = true;
    }

    private boolean isObservation() {
        return observation;
    }

    public boolean isShowDefault() {
        return showDefault;
    }

    public void setShowDefault(final String showDefault) {
        this.showDefault = !StringUtils.equalsIgnoreCase(showDefault, "false");
    }

    public String getSource() {
        return source;
    }

    private void setSource(final String source) {
        this.source = source;
    }

    public String getSortBy() {
        return sortBy;
    }

    private void setSortBy(final String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    private void setSortOrder(final String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getSortComparator() {
        return sortComparator;
    }

    private void setSortComparator(final String sortComparator) {
        this.sortComparator = sortComparator;
    }

    public String getValueListProvider() {
        return valueListProvider;
    }

    public void setValueListProvider(final String valueListProvider) {
        this.valueListProvider = valueListProvider;
    }
}

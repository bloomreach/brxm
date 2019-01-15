/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.PropertyType;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.forge.selection.frontend.plugin.Config;

public class RadioGroupFieldType extends PrimitiveFieldType {

    private String source = null;
    private String sortComparator = null;
    private String sortBy = null;
    private String sortOrder = null;
    private String orientation = null;

    public RadioGroupFieldType() {
        setType(Type.RADIO_GROUP);
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        fieldContext.getStringConfig(Config.SOURCE).ifPresent(this::setSource);
        fieldContext.getStringConfig(Config.SORT_COMPARATOR).ifPresent(this::setSortComparator);
        fieldContext.getStringConfig(Config.SORT_BY).ifPresent(this::setSortBy);
        fieldContext.getStringConfig(Config.SORT_ORDER).ifPresent(this::setSortOrder);
        fieldContext.getStringConfig(Config.ORIENTATION).ifPresent(this::setOrientation);
        return super.init(fieldContext);
    }

    @Override
    protected int getPropertyType() {
        return PropertyType.STRING;
    }

    @Override
    protected String getDefault() {
        return StringUtils.EMPTY;
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

    public String getOrientation() {
        return orientation;
    }

    private void setOrientation(final String orientation) {
        this.orientation = orientation;
    }
}

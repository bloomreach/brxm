/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.jcr.PropertyType;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;

public class StaticDropdownFieldType extends PrimitiveFieldType {
    
    private List<String> optionValues = new LinkedList<>();
    private List<String> optionDisplayValues = new LinkedList<>();
    
    public StaticDropdownFieldType() {
        setType(Type.STATIC_DROPDOWN);
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        final Optional<String> selectableOptions = fieldContext.getStringConfig("selectable.options");

        selectableOptions.ifPresent(allOptions -> Stream.of(allOptions.split(","))
            .forEach(oneOption -> {
                if (oneOption.contains("=")) {
                    optionValues.add(StringUtils.substringBefore(oneOption, "="));
                    optionDisplayValues.add(StringUtils.substringAfter(oneOption, "="));
                } else {
                    optionValues.add(oneOption);
                    optionDisplayValues.add(oneOption);
                }
            }));
        
        return super.init(fieldContext);
    }

    public List<String> getOptionValues() {
        return optionValues;
    }

    public List<String> getOptionDisplayValues() {
        return optionDisplayValues;
    }

    @Override
    protected int getPropertyType() {
        return PropertyType.STRING;
    }

    @Override
    protected String getDefault() {
        return StringUtils.EMPTY;
    }

    @Override
    protected Object getValidatedValue(final FieldValue value) {
        return value.getValue();
    }
}

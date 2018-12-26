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
import java.util.Locale;
import java.util.Optional;

import javax.jcr.PropertyType;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.valuelist.ValueListService;
import org.onehippo.forge.selection.frontend.plugin.Config;

public class RadioGroupFieldType extends PrimitiveFieldType {

    private List<String> buttonValues = new LinkedList<>();
    private List<String> buttonDisplayValues = new LinkedList<>();

    public RadioGroupFieldType() {
        setType(Type.RADIO_GROUP);
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        final Optional<String> source = fieldContext.getStringConfig(Config.SOURCE);
        
        // TODO: check if a custom provider will work with this code & use it correctly if we can
        // final Optional<String> valueListProvider = fieldContext.getStringConfig(Config.VALUELIST_PROVIDER);
        
        // TODO: deal with the sorting and orientation paramaters of the field configuration
        final Locale locale = fieldContext.getParentContext().getLocale();
        final Session session = fieldContext.getParentContext().getSession();

        source.ifPresent(src -> ValueListService.get().getValueList(src, locale, session)
                .forEach(listItem -> {
                    buttonValues.add(listItem.getKey());
                    buttonDisplayValues.add(listItem.getLabel());
                }));                

        return super.init(fieldContext);
    }

    public List<String> getButtonValues() {
        return buttonValues;
    }

    public List<String> getButtonDisplayValues() {
        return buttonDisplayValues;
    }

    @Override
    protected int getPropertyType() {
        return PropertyType.STRING;
    }

    @Override
    protected String getDefault() {
        return StringUtils.EMPTY;
    }
}

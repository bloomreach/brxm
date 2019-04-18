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
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.forge.selection.frontend.plugin.Config;

public class BooleanRadioGroupFieldType extends BooleanFieldType {

    private String falseLabel = "false";
    private String orientation = null;
    private String source = null;
    private String trueLabel = "true";

    public BooleanRadioGroupFieldType() {
        setType(Type.BOOLEAN_RADIO_GROUP);
    }
    
    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        fieldContext.getStringConfig(Config.FALSE_LABEL)
                .ifPresent(value -> setFalseLabel(StringUtils.defaultIfBlank(value, falseLabel)));
        fieldContext.getStringConfig(Config.ORIENTATION).ifPresent(this::setOrientation);
        fieldContext.getStringConfig(Config.SOURCE).ifPresent(this::setSource);
        fieldContext.getStringConfig(Config.TRUE_LABEL)
                .ifPresent(value -> setTrueLabel(StringUtils.defaultIfBlank(value, trueLabel)));

        return super.init(fieldContext);
    }

    public String getFalseLabel() {
        return falseLabel;
    }

    public void setFalseLabel(final String falseLabel) {
        this.falseLabel = falseLabel;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(final String orientation) {
        this.orientation = orientation;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }
    public String getTrueLabel() {
        return trueLabel;
    }

    public void setTrueLabel(final String trueLabel) {
        this.trueLabel = trueLabel;
    }
}

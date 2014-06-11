/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.selection;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiModel;

import org.onehippo.cms7.essentials.dashboard.model.Restful;

@ApiModel
@XmlRootElement(name = "selectionField")
public class SelectionFieldRestful implements Restful{

    private static final long serialVersionUID = 1L;
    private String nameSpace;
    private String documentName;
    private String fieldName;
    private String fieldPosition;
    private String selectionType;
    private String valueList;

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(final String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(final String documentName) {
        this.documentName = documentName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldPosition() {
        return fieldPosition;
    }

    public void setFieldPosition(final String fieldPosition) {
        this.fieldPosition = fieldPosition;
    }

    public String getSelectionType() {
        return selectionType;
    }

    public void setSelectionType(final String selectionType) {
        this.selectionType = selectionType;
    }

    public String getValueList() {
        return valueList;
    }

    public void setValueList(final String valueList) {
        this.valueList = valueList;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SelectionFieldRestful{");
        sb.append("nameSpace='").append(nameSpace).append('\'');
        sb.append(", documentName='").append(documentName).append('\'');
        sb.append(", fieldName='").append(fieldName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.rest.beans;

import java.io.Serializable;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;

/**
 * An information class for {@link FieldGroup}
 * 
 * This class is used to serialize information about different {@link FieldGroup} in an {@link FieldGroupList}
 * because Java annotations cannot be serialized over the wire directly.
 */
public class FieldGroupInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String[] value;
    String titleKey;

    public FieldGroupInfo() {
        titleKey = "";
    }

    public FieldGroupInfo(String[] value, String titleKey) {
        this.value = value;
        this.titleKey = titleKey;
    }

    /**
     * Retrieve {@link FieldGroup} value
     * 
     * @return The value of a {@link FieldGroup} annotation
     */
    public String[] getValue() {
        return value;
    }

    /**
     * Set the value of a {@link FieldGroup} annotation
     * 
     * @param value - The value to set to a {@link FieldGroup}
     */
    public void setValue(String[] value) {
        this.value = value;
    }

    /**
     * Retrieve {@link FieldGroup} title key
     * 
     * @return the titleKey
     */
    public String getTitleKey() {
        return titleKey;
    }

    /**
     * Set {@link FieldGroup} title key
     * 
     * @param titleKey the titleKey to set
     */
    public void setTitleKey(String titleKey) {
        this.titleKey = titleKey;
    }

}

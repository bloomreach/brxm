/*
 *  Copyright 2012 Hippo.
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

import java.util.List;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.parameters.FieldGroupList;

/**
 * Class to hold information about {@link ChannelInfo} classes
 */
public class ChannelInfoClassInfo {

    private String className;
    private List<FieldGroupInfo> fieldsGroup;

    /**
     * {@link ChannelInfoClassInfo} default constructor
     */
    public ChannelInfoClassInfo() {
    }

    public ChannelInfoClassInfo(String className, List<FieldGroupInfo> fieldsGroup) {
        this.className = className;
        this.fieldsGroup = fieldsGroup;
    }

    /**
     * Retrieve class name
     * 
     * @return The className
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set class name
     * 
     * @param className - The className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Retrieve {@link FieldGroupList}
     * 
     * @return The fieldsGroup
     */
    public List<FieldGroupInfo> getFieldsGroup() {
        return fieldsGroup;
    }

    /**
     * Set {@link FieldGroupList}
     * 
     * @param fieldsGroup the fieldsGroup to set
     */
    public void setFieldsGroup(List<FieldGroupInfo> fieldsGroup) {
        this.fieldsGroup = fieldsGroup;
    }

}

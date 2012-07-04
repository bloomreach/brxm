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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.JcrPath;

/**
 * Class to build different objects information to be easily serialized over the wire with different REST calls
 */
public final class InformationObjectsBuilder {

    public static ChannelInfoClassInfo buildChannelInfoClassInfo(Class<? extends ChannelInfo> channelInfoClass) {
        ChannelInfoClassInfo channelInfoClassInfo = new ChannelInfoClassInfo();

        channelInfoClassInfo.setClassName(channelInfoClass.getName());
        channelInfoClassInfo.setFieldsGroup(buildFieldGroupListInfo(channelInfoClass.getAnnotation(FieldGroupList.class)));
        return channelInfoClassInfo;
    }

    public static List<FieldGroupInfo> buildFieldGroupListInfo(FieldGroupList fieldsGroup) {
        List<FieldGroupInfo> fieldsGroupList = new ArrayList<FieldGroupInfo>();

        if (fieldsGroup != null) {
            for (FieldGroup fieldGroup : fieldsGroup.value()) {
                try {
                    fieldsGroupList.add(buildFieldGroupInfo(fieldGroup));
                } catch (IllegalArgumentException iae) {
                }
            }
        }

        return fieldsGroupList; 
    }

    public static FieldGroupInfo buildFieldGroupInfo(FieldGroup fieldGroup) {
        if (fieldGroup == null) {
            throw new IllegalArgumentException("Got a 'null' field group argument!");
        }

        FieldGroupInfo fieldGroupInfo = new FieldGroupInfo();

        fieldGroupInfo.setValue(fieldGroup.value());
        fieldGroupInfo.setTitleKey(fieldGroup.titleKey());
        return fieldGroupInfo;
    }

    public static List<HstPropertyDefinitionInfo> buildHstPropertyDefinitionInfos(List<HstPropertyDefinition> hstPropertyDefinitions) {
        List<HstPropertyDefinitionInfo> hstPropertyDefinitionInfos = new ArrayList<HstPropertyDefinitionInfo>(hstPropertyDefinitions.size());

        for (HstPropertyDefinition hstPropertyDefinition : hstPropertyDefinitions) {
            hstPropertyDefinitionInfos.add(buildHstPropertyDefinitionInfo(hstPropertyDefinition));
        }

        return hstPropertyDefinitionInfos;
    }

    @SuppressWarnings("unchecked")
    public static HstPropertyDefinitionInfo buildHstPropertyDefinitionInfo(HstPropertyDefinition hstPropertyDefinition) {
        HstPropertyDefinitionInfo hstPropertyDefinitionInfo = new HstPropertyDefinitionInfo();

        hstPropertyDefinitionInfo.setIsRequired(hstPropertyDefinition.isRequired());
        hstPropertyDefinitionInfo.setDefaultValue(hstPropertyDefinition.getDefaultValue());
        hstPropertyDefinitionInfo.setName(hstPropertyDefinition.getName());
        hstPropertyDefinitionInfo.setValueType(hstPropertyDefinition.getValueType());
        hstPropertyDefinitionInfo.setAnnotations(hstPropertyDefinition.getAnnotations(Arrays.asList(ImageSetPath.class, JcrPath.class, DropDownList.class)));
        return hstPropertyDefinitionInfo;
    }

    public static Properties buildResourceBundleProperties(ResourceBundle resourceBundle) {
        Properties properties = new Properties();

        for (String key : resourceBundle.keySet()) {
            properties.put(key, resourceBundle.getString(key));
        }

        return properties;
    }

}

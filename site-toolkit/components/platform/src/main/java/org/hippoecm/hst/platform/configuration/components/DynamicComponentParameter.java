/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.components;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.hippoecm.hst.configuration.components.DropdownListParameterConfig;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.DynamicParameterConfig;
import org.hippoecm.hst.configuration.components.ImageSetPathParameterConfig;
import org.hippoecm.hst.configuration.components.JcrPathParameterConfig;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.EmptyValueListProvider;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ValueListProvider;
import org.hippoecm.hst.provider.ValueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class DynamicComponentParameter implements DynamicParameter{

    private static final Logger log = LoggerFactory.getLogger(DynamicComponentParameter.class);
    public static final String HST_REQUIRED = "hst:required";
    public static final String HST_DEFAULT_VALUE = "hst:defaultvalue";
    public static final String HST_DISPLAY_NAME = "hst:displayname";
    public static final String HST_HIDE_IN_CHANNEL_MANAGER = "hst:hideinchannelmanager";
    public static final String HST_FIELD_GROUP = "hst:fieldgroup";
    public static final String HST_VALUE_TYPE = "hst:valuetype";
    public static final String HST_JCRPATH_TYPE = "hst:jcrpath";
    public static final String HST_IMAGESETPATH_TYPE = "hst:imagesetpath";
    public static final String HST_DROPDOWNLIST_TYPE = "hst:dropdown";
    public static final String DEFAULT_STRING_TYPE = "STRING";

    public static final String DEFAULT_CMS_PICKERS_DOCUMENTS = "cms-pickers/documents";
    public static final String PICKER_CONFIGURATION = "hst:pickerconfiguration";
    public static final String PICKER_INITIAL_PATH = "hst:pickerinitialpath";
    public static final String PICKER_REMEMBERS_LAST_VISITED = "hst:pickerrememberslastvisited";
    public static final String PICKER_SELECTABLE_NODE_TYPES = "hst:pickerselectablenodetypes";
    
    public static class JcrPathParameterConfigImpl implements JcrPathParameterConfig {

        public static final String RELATIVE = "hst:relative";
        public static final String PICKER_ROOT_PATH = "hst:pickerrootpath";

        private final String pickerConfiguration;
        private final String pickerInitialPath;
        private final boolean pickerRemembersLastVisited;
        private final String[] pickerSelectableNodeTypes;
        private final boolean relative;
        private final String pickerRootPath;

        public JcrPathParameterConfigImpl(final JcrPath annotation) {
            pickerConfiguration = annotation.pickerConfiguration();
            pickerInitialPath = annotation.pickerInitialPath();
            pickerRemembersLastVisited = annotation.pickerRemembersLastVisited();
            pickerSelectableNodeTypes = annotation.pickerSelectableNodeTypes();
            relative = annotation.isRelative();
            pickerRootPath = annotation.pickerRootPath();
        }

		public JcrPathParameterConfigImpl(final HstNode jcrPathNode) {
            final ValueProvider valueProvider = jcrPathNode.getValueProvider();
            pickerConfiguration = ofNullable(valueProvider.getString(PICKER_CONFIGURATION))
                    .orElse(DEFAULT_CMS_PICKERS_DOCUMENTS);
			pickerInitialPath = ofNullable(valueProvider.getString(PICKER_INITIAL_PATH))
                    .orElse(EMPTY);
			pickerRemembersLastVisited = ofNullable(valueProvider.getBoolean(PICKER_REMEMBERS_LAST_VISITED))
                    .orElse(true);
			pickerSelectableNodeTypes = ofNullable(valueProvider.getStrings(PICKER_SELECTABLE_NODE_TYPES))
                    .orElse(new String[]{});
			relative = ofNullable(valueProvider.getBoolean(RELATIVE))
                    .orElse(false);
			pickerRootPath = ofNullable(valueProvider.getString(PICKER_ROOT_PATH)).orElse(EMPTY);
		}

        public String getPickerConfiguration() {
            return pickerConfiguration;
        }

        public String getPickerInitialPath() {
            return pickerInitialPath;
        }

        public boolean isPickerRemembersLastVisited() {
            return pickerRemembersLastVisited;
        }

        public String[] getPickerSelectableNodeTypes() {
            return pickerSelectableNodeTypes;
        }

        public boolean isRelative() {
            return relative;
        }

        public String getPickerRootPath() {
            return pickerRootPath;
        }

        
        @Override
        public String toString() {
            return "JcrPathProperty{" +
                    "pickerConfiguration='" + pickerConfiguration + '\'' +
                    ", pickerInitialPath='" + pickerInitialPath + '\'' +
                    ", pickerRemembersLastVisited=" + pickerRemembersLastVisited +
                    ", pickerSelectableNodeTypes=" + Arrays.toString(pickerSelectableNodeTypes) +
                    ", relative=" + relative +
                    ", pickerRootPath='" + pickerRootPath + '\'' +
                    '}';
        }
    }

    public static class ImageSetPathParameterConfigImpl implements ImageSetPathParameterConfig {

        public static final String PREVIEW_VARIANT = "hst:previewvariant";

        private final String previewVariant;
        private final String pickerConfiguration;
        private final String pickerInitialPath;
        private final boolean pickerRemembersLastVisited;
        private final String[] pickerSelectableNodeTypes;

        public ImageSetPathParameterConfigImpl(final ImageSetPath annotation) {
            previewVariant = annotation.previewVariant();
            pickerConfiguration = annotation.pickerConfiguration();
            pickerInitialPath = annotation.pickerInitialPath();
            pickerRemembersLastVisited = annotation.pickerRemembersLastVisited();
            pickerSelectableNodeTypes = annotation.pickerSelectableNodeTypes();
        }

        public ImageSetPathParameterConfigImpl(final HstNode jcrPathNode) {
            final ValueProvider valueProvider = jcrPathNode.getValueProvider();
            previewVariant = ofNullable(valueProvider.getString(PREVIEW_VARIANT))
                    .orElse("");
            pickerConfiguration = ofNullable(valueProvider.getString(PICKER_CONFIGURATION))
                    .orElse(DEFAULT_CMS_PICKERS_DOCUMENTS);
            pickerInitialPath = ofNullable(valueProvider.getString(PICKER_INITIAL_PATH))
                    .orElse(EMPTY);
            pickerRemembersLastVisited = ofNullable(valueProvider.getBoolean(PICKER_REMEMBERS_LAST_VISITED))
                    .orElse(true);
            pickerSelectableNodeTypes = ofNullable(valueProvider.getStrings(PICKER_SELECTABLE_NODE_TYPES))
                    .orElse(new String[]{});

        }

        public String getPreviewVariant() {
            return previewVariant;
        }

        public String getPickerConfiguration() {
            return pickerConfiguration;
        }

        public String getPickerInitialPath() {
            return pickerInitialPath;
        }

        public boolean isPickerRemembersLastVisited() {
            return pickerRemembersLastVisited;
        }

        public String[] getPickerSelectableNodeTypes() {
            return pickerSelectableNodeTypes;
        }

        @Override
        public String toString() {
            return "ImageSetPath{" +     
                    "previewVariant='" + previewVariant + '\'' +
                    "pickerConfiguration='" + pickerConfiguration + '\'' +
                    ", pickerInitialPath='" + pickerInitialPath + '\'' +
                    ", pickerRemembersLastVisited=" + pickerRemembersLastVisited +
                    ", pickerSelectableNodeTypes=" + Arrays.toString(pickerSelectableNodeTypes) +
                    '}';
        }
    }
    
    
    public static class DropdownListParameterConfigImpl implements DropdownListParameterConfig {
        public static final String VALUE = "hst:value";
        public static final String VALUE_LIST_PROVIDER = "hst:valuelistprovider";

        private final String[] values;
        private Class<? extends ValueListProvider> valueListProvider = EmptyValueListProvider.class;

        public DropdownListParameterConfigImpl(final DropDownList annotation) {
            values = annotation.value();
            valueListProvider = annotation.valueListProvider();
        }

        public DropdownListParameterConfigImpl(final HstNode dropdownNode) {
            final ValueProvider valueProvider = dropdownNode.getValueProvider();
            try {
                final Class<? extends ValueListProvider> valueListProviderClass = (Class<? extends ValueListProvider>) Class
                        .forName(valueProvider.getString(VALUE_LIST_PROVIDER));
                if (valueListProviderClass != null) {
                    valueListProvider = valueListProviderClass;
                }
            } catch (ClassNotFoundException e) {
                log.warn("The class name defined in hst:valueListProvider property is not found: {}",
                        valueProvider.getString(VALUE_LIST_PROVIDER));
            }
            values = ofNullable(valueProvider.getStrings(VALUE)).orElse(new String[] {});
        }

        public String[] getValues() {
            return values;
        }

        public Class<? extends ValueListProvider> getValueListProvider() {
            return valueListProvider;
        }
    }

    private final String name;
    private final boolean required;
    private final String defaultValue;
    private final String displayName;
    private final boolean hideInChannelManager;
    private final String fieldGroup;
    private final String valueType;
    private final boolean residual;

    private DynamicParameterConfig hstComponentParameterConfig;

    //TODO SS: Add more constructors, or introduce a factory, residual property should be not controlled by constructor!!!
	public DynamicComponentParameter(final HstNode parameterNode) {
        final ValueProvider valueProvider = parameterNode.getValueProvider();
        name = valueProvider.getName();
		required = ofNullable(valueProvider.getBoolean(HST_REQUIRED)).orElse(false);
        valueType = ofNullable(valueProvider.getString(HST_VALUE_TYPE)).orElse(DEFAULT_STRING_TYPE);
        defaultValue = ofNullable(valueProvider.getString(HST_DEFAULT_VALUE)).orElse(EMPTY);
        displayName = ofNullable(valueProvider.getString(HST_DISPLAY_NAME)).orElse(EMPTY);
        hideInChannelManager = ofNullable(valueProvider.getBoolean(HST_HIDE_IN_CHANNEL_MANAGER)).orElse(false);
        fieldGroup = valueProvider.getString(HST_FIELD_GROUP);
        residual = true;

        for (final HstNode childNode : parameterNode.getNodes()) {
            if (childNode.getNodeTypeName().equals(HST_JCRPATH_TYPE)) {
                hstComponentParameterConfig = new JcrPathParameterConfigImpl(childNode);
            } else if (childNode.getNodeTypeName().equals(HST_IMAGESETPATH_TYPE)) {
                hstComponentParameterConfig = new ImageSetPathParameterConfigImpl(childNode);
            } else if (childNode.getNodeTypeName().equals(HST_DROPDOWNLIST_TYPE)) {
                hstComponentParameterConfig = new DropdownListParameterConfigImpl(childNode);
            }
            if (hstComponentParameterConfig != null) {
                // Only one property is allowed
                break;
            }
        }
	}

    public DynamicComponentParameter(final Parameter parameter, final Method method) {
        name = parameter.name();
        required = parameter.required();
        defaultValue = parameter.defaultValue();
        hideInChannelManager = parameter.hideInChannelManager();
        displayName = parameter.displayName();
        fieldGroup = null;
        residual = false;

        String customType = null;
        for (final Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType() == JcrPath.class) {
                hstComponentParameterConfig = new DynamicComponentParameter.JcrPathParameterConfigImpl(
                        (JcrPath) annotation);
                customType = DynamicParameterType.JCR_PATH.name();
            } else if (annotation.annotationType() == ImageSetPath.class) {
                hstComponentParameterConfig = new ImageSetPathParameterConfigImpl((ImageSetPath) annotation);
                //TODO SS: Handle ImageSetPath type
            } else if (annotation.annotationType() == DropDownList.class) {
                hstComponentParameterConfig = new DropdownListParameterConfigImpl((DropDownList) annotation);
                customType = DynamicParameterType.VALUE_FROM_LIST.name();
            }
            if (hstComponentParameterConfig != null) {
                // Only one property is allowed
                break;
            }
        }

        valueType = customType != null ? customType : DynamicParameterType.getType(method, parameter).name();
    }

    //Test Constructor
    public DynamicComponentParameter(final Parameter parameter, String parameterValueType) {
        name = parameter.name();
        required = parameter.required();
        defaultValue = parameter.defaultValue();
        hideInChannelManager = parameter.hideInChannelManager();
        displayName = parameter.displayName();
        fieldGroup = null;
        residual = false;
        valueType = parameterValueType;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isHideInChannelManager() {
        return hideInChannelManager;
    }

    public String getFieldGroup() {
        return fieldGroup;
    }

    public String getValueType() {
        return valueType;
    }

    public DynamicParameterConfig getComponentParameterConfig() {
        return hstComponentParameterConfig;
    }

    @Override
    public String toString() {
        return "HstComponentParameterImpl{" +
                "name='" + name + '\'' +
                ", required=" + required +
                ", valueType='" + valueType + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", displayName='" + displayName + '\'' +
                ", hideInChannelManager=" + hideInChannelManager +
                ", fieldGroup='" + fieldGroup + '\'' +
                ", hstComponentParameterConfig=" + hstComponentParameterConfig +
                '}';
    }

    @Override
    public boolean isResidual() {
        return residual;
    }
}

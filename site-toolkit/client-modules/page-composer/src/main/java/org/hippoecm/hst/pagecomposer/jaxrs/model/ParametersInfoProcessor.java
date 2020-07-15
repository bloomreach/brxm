/*
 *  Copyright 2011-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.DropdownListParameterConfig;
import org.hippoecm.hst.configuration.components.DynamicFieldGroup;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.DynamicParameterConfig;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.ImageSetPathParameterConfig;
import org.hippoecm.hst.configuration.components.JcrPathParameterConfig;
import org.hippoecm.hst.configuration.components.ParameterValueType;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.EmptyValueListProvider;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.parameters.ValueListProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.DocumentUtils;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.hippoecm.hst.platform.configuration.components.DynamicComponentParameter;
import org.hippoecm.hst.platform.configuration.components.ResourceBundleListProvider;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public abstract class ParametersInfoProcessor {

    private static final Logger log = LoggerFactory.getLogger(ParametersInfoProcessor.class);

    public static final String HINT_POSTFIX = ".hint";
    private static final String COMPONENT_PARAMETERS_TRANSLATION_LOCATION = "hippo:hst.componentparameters";

    private static final Set<CacheKey> FAILED_BUNDLES_TO_LOAD = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static void setParameterType(final ContainerItemComponentPropertyRepresentation property,
            final DynamicParameter jcrComponentParameter) {
        if (jcrComponentParameter.getValueType() == null) {
            return;
        }

        final ParameterValueType valueType = jcrComponentParameter.getValueType();

        final DynamicParameterConfig componentParameterConfig = jcrComponentParameter.getComponentParameterConfig();
        if (componentParameterConfig != null && componentParameterConfig.getType() == DynamicParameterConfig.Type.JCR_PATH
                && DynamicParameterConfig.Type.JCR_PATH.supportsReturnType(valueType)) {
            property.setType(ParameterType.JCR_PATH);
        } else if (componentParameterConfig != null && componentParameterConfig.getType() == DynamicParameterConfig.Type.DROPDOWN_LIST
                && DynamicParameterConfig.Type.DROPDOWN_LIST.supportsReturnType(valueType)) {
            property.setType(ParameterType.VALUE_FROM_LIST);
        } else if (componentParameterConfig != null && componentParameterConfig.getType() == DynamicParameterConfig.Type.IMAGESET_PATH
                && DynamicParameterConfig.Type.IMAGESET_PATH.supportsReturnType(valueType)) {
            //TODO Handle image set value type
            property.setType(ParameterType.STRING);
        } else if (jcrComponentParameter.getValueType() == ParameterValueType.INTEGER
                || jcrComponentParameter.getValueType() == ParameterValueType.DECIMAL) {
            property.setType(ParameterType.NUMBER);
        } else {
            try {
                property.setType(ParameterType.valueOf(jcrComponentParameter.getValueType().name()));
            } catch (IllegalArgumentException e) {
                log.error("Dynamic parameter value type is not matched with any frontend parameter type: {} ",
                        jcrComponentParameter.getValueType().name());
                property.setType(ParameterType.STRING);
            }
        }
    }

    public static List<ContainerItemComponentPropertyRepresentation> getPopulatedProperties(
            final Class<?> infoClassType,
            final Locale locale,
            final String contentPath,
            final String prefix,
            final Node containerItemNode,
            final ContainerItemHelper containerItemHelper,
            final List<PropertyRepresentationFactory> propertyPresentationFactories,
            final HstComponentConfiguration componentReference) throws RepositoryException {

        final ResourceBundle[] resourceBundles = getResourceBundles(infoClassType, locale, componentReference);

        final Map<String, ContainerItemComponentPropertyRepresentation> propertyMap = new LinkedHashMap<>();

        for (final DynamicParameter jcrComponentParameter : componentReference.getDynamicComponentParameters()) {
            final ContainerItemComponentPropertyRepresentation property = new ContainerItemComponentPropertyRepresentation();
            property.setName(jcrComponentParameter.getName());
            property.setDefaultValue(jcrComponentParameter.getDefaultValue());
            property.setRequired(jcrComponentParameter.isRequired());
            property.setHiddenInChannelManager(jcrComponentParameter.isHideInChannelManager());
            setParameterType(property, jcrComponentParameter);

            final DynamicParameterConfig componentParameterConfig = jcrComponentParameter.getComponentParameterConfig();
            if (componentParameterConfig != null) {
                if (componentParameterConfig.getType() == DynamicParameterConfig.Type.JCR_PATH) {
                    populateJcrPathProperties(property, (JcrPathParameterConfig) componentParameterConfig, contentPath);
                } else if (componentParameterConfig.getType() == DynamicParameterConfig.Type.DROPDOWN_LIST) {
                    populateDropdownListProperties(property, (DropdownListParameterConfig) componentParameterConfig,
                            resourceBundles, locale);
                } else if (componentParameterConfig.getType() == DynamicParameterConfig.Type.IMAGESET_PATH) {
                    populateImageSetPathProperties(property, (ImageSetPathParameterConfig) componentParameterConfig);
                }
            }

            String label = getResourceValue(resourceBundles, jcrComponentParameter.getName(), null);
            if (label == null) {
                label = isEmpty(jcrComponentParameter.getDisplayName()) ? jcrComponentParameter.getName()
                        : jcrComponentParameter.getDisplayName();
            }
            property.setLabel(label);
            property.setHint(getResourceValue(resourceBundles, jcrComponentParameter.getName() + HINT_POSTFIX, null));

            propertyMap.put(property.getName(), property);
        }


        final List<ContainerItemComponentPropertyRepresentation> properties = orderParametersByFieldGroup(componentReference, propertyMap, resourceBundles);

        final HstComponentParameters componentParameters = new HstComponentParameters(containerItemNode, containerItemHelper);

        setValueForProperties(properties, prefix, componentParameters, contentPath);

        if (propertyPresentationFactories != null) {
            int index = 0;
            for (final PropertyRepresentationFactory factory : propertyPresentationFactories) {
                try {
                    final ContainerItemComponentPropertyRepresentation property = factory.createProperty(
                            locale, contentPath, prefix, containerItemNode, containerItemHelper, componentParameters, 
                            properties);
                    if (property != null) {
                        properties.add(index, property);
                        index++;
                    }
                } catch (final RuntimeException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("PropertyRepresentationFactory '{}' threw exception.", factory.getClass().getName(),
                                e);
                    } else {
                        log.warn("PropertyRepresentationFactory '{}' threw exception: {}", factory.getClass().getName(), 
                                e.toString());
                    }
                }
            }
        }

        return properties;
    }

    /**
     * Method used by downstream projects.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static void setValueForProperties(final List<ContainerItemComponentPropertyRepresentation> properties,
                                             final String prefix,
                                             final HstComponentParameters componentParameters) {
        setValueForProperties(properties, prefix, componentParameters, null);
    }

    public static void setValueForProperties(final Collection<ContainerItemComponentPropertyRepresentation> properties,
                                             final String prefix, final HstComponentParameters componentParameters, 
                                             final String contentPath) {
        for (final ContainerItemComponentPropertyRepresentation prop : properties) {
            setValueForProperty(prop, prefix, componentParameters, contentPath);
        }
    }

    /**
     * Method used by downstream projects.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static void setValueForProperty(final ContainerItemComponentPropertyRepresentation property,
                                           final String prefix,
                                           final HstComponentParameters componentParameters) {
        setValueForProperty(property, prefix, componentParameters, null);
    }

    public static void setValueForProperty(final ContainerItemComponentPropertyRepresentation property,
                                           final String prefix,
                                           final HstComponentParameters componentParameters, final String contentPath) {
        
        final String value = componentParameters.getValue(prefix, property.getName());
        if (value != null && !value.isEmpty()) {
            property.setValue(value);
            if (isNotEmpty(contentPath) && property.getType().equals(ParameterType.JCR_PATH.xtype)) {
                final String absPath = value.startsWith("/") ? value : contentPath + "/" + value;
                final DocumentRepresentation docRepresentation =
                        DocumentUtils.getDocumentRepresentationHstConfigUser(absPath);
                final String displayName = docRepresentation.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    property.setDisplayValue(displayName);
                } else if (docRepresentation.getPath() != null) {
                    log.debug("Could not retrieve displayName for path '{}'. Possibly a removed node. Set the last " +
                            "path segment as displayValue instead.", docRepresentation.getPath());
                    property.setDisplayValue(StringUtils.substringAfterLast(docRepresentation.getPath(), "/"));
                }
            }
        }
    }

    private static Map<String, ContainerItemComponentPropertyRepresentation> createPropertyMap(final String contentPath,
            final Class<?> classType, final ResourceBundle[] resourceBundles, final Locale locale) {
        // although below the classType.getMethods() returns methods in random order (not in jdk 6 but it does in jdk 7 
        // which is according spec) we still create a LinkedHashMap because for jdk6 this works. For jdk 7, developers 
        // must (can only) use FieldGroup annotation to specify the order of the component properties.
        
        final Map<String, ContainerItemComponentPropertyRepresentation> propertyMap = new LinkedHashMap<>();
        for (final Method method : classType.getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                final Parameter propAnnotation = method.getAnnotation(Parameter.class);
                final ContainerItemComponentPropertyRepresentation prop = 
                        new ContainerItemComponentPropertyRepresentation();
                prop.setName(propAnnotation.name());
                prop.setDefaultValue(propAnnotation.defaultValue());
                prop.setDescription(propAnnotation.description());
                prop.setRequired(propAnnotation.required());
                prop.setHiddenInChannelManager(propAnnotation.hideInChannelManager());

                String label = getResourceValue(resourceBundles, propAnnotation.name(), null);
                if (label == null) {
                    if (propAnnotation.displayName().equals("")) {
                        label = propAnnotation.name();
                    } else {
                        label = propAnnotation.displayName();
                    }
                }
                prop.setLabel(label);
                prop.setHint(getResourceValue(resourceBundles, propAnnotation.name() + HINT_POSTFIX, null));

                final Annotation annotation = ParameterType.getTypeAnnotation(method);
                if (annotation instanceof JcrPath) {
                    // for JcrPath we need some extra processing
                    final JcrPathParameterConfig jcrPath = new DynamicComponentParameter.JcrPathParameterConfigImpl(
                            (JcrPath) annotation);
                    populateJcrPathProperties(prop, jcrPath, contentPath);
                } else if (annotation instanceof DropDownList) {
                    final DropdownListParameterConfig dropdownList = new DynamicComponentParameter.DropdownListParameterConfigImpl(
                            (DropDownList) annotation);
                    populateDropdownListProperties(prop, dropdownList, resourceBundles, locale);
                } else if (annotation instanceof ImageSetPath) {
                    final ImageSetPathParameterConfig imageSetPath = new DynamicComponentParameter.ImageSetPathParameterConfigImpl(
                            (ImageSetPath) annotation);
                    populateImageSetPathProperties(prop, imageSetPath);
                }

                final ParameterType type = ParameterType.getType(method, annotation);
                prop.setType(type);

                propertyMap.put(prop.getName(), prop);
            }
        }
        return propertyMap;
    }
    
    private static void populateDropdownListProperties(final ContainerItemComponentPropertyRepresentation prop,
            final DropdownListParameterConfig dropdownList, final ResourceBundle[] resourceBundles, final Locale locale) {
        
        
        String values[] = dropdownList.getValues();

        ValueListProvider valueListProvider = null;
        final Class<?> valueListProviderClass = dropdownList.getValueListProvider();
        final String sourceId = dropdownList.getSourceId();

        if (StringUtils.isNotEmpty(sourceId)
                && (valueListProviderClass == null || EmptyValueListProvider.class.equals(valueListProviderClass))) {
            valueListProvider = new ResourceBundleListProvider(dropdownList.getSourceId());
        }

        if (valueListProviderClass != null && !EmptyValueListProvider.class.equals(valueListProviderClass)) {
            try {
                if (!StringUtils.isEmpty(sourceId)) {
                    try {
                        valueListProvider = (ValueListProvider) valueListProviderClass.getConstructor(String.class)
                                .newInstance(sourceId);
                    } catch (NoSuchMethodException e) {
                        log.warn(
                                "ValueListProvider class constructor with String parameter does not exist. SourceId: {} ValueListProvider: {}",
                                sourceId, valueListProviderClass);
                    }
                }
                if (valueListProvider == null) {
                    valueListProvider = (ValueListProvider) valueListProviderClass.newInstance();
                }
            } catch (final Exception e) {
                log.error("Failed to create or invoke the custom valueListProvider: '{}'.", valueListProviderClass, e);
            }
        }

        final String[] displayValues;

        if (valueListProvider == null) {
            displayValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                displayValues[i] = getResourceSubValue(resourceBundles, prop.getName(), values[i]);
            }
        } else {
            final List<String> valueList = valueListProvider.getValues();
            values = valueList.toArray(new String[valueList.size()]);
            displayValues = new String[values.length];
            String value;
            String displayValue;
            for (int i = 0; i < values.length; i++) {
                value = values[i];
                displayValue = valueListProvider.getDisplayValue(value, locale);
                displayValues[i] = (displayValue != null) ? displayValue : value;
            }
        }

        prop.setDropDownListValues(values);
        prop.setDropDownListDisplayValues(displayValues);
        
    }
 
    private static void populateImageSetPathProperties(final ContainerItemComponentPropertyRepresentation prop,
            final ImageSetPathParameterConfig imageSet) {
        prop.setPickerConfiguration(imageSet.getPickerConfiguration());
        prop.setPickerInitialPath(imageSet.getPickerInitialPath());
        prop.setPickerRemembersLastVisited(imageSet.isPickerRemembersLastVisited());
        prop.setPickerSelectableNodeTypes(imageSet.getPickerSelectableNodeTypes());
    }
 
    private static void populateJcrPathProperties(final ContainerItemComponentPropertyRepresentation prop,
                                                  final JcrPathParameterConfig jcrPath, final String contentPath) {
        prop.setPickerConfiguration(jcrPath.getPickerConfiguration());
        prop.setPickerInitialPath(jcrPath.getPickerInitialPath());
        String pickerRootPath = jcrPath.getPickerRootPath();
        if (StringUtils.isEmpty(pickerRootPath)) {
            pickerRootPath = contentPath;
        }
        prop.setPickerRootPath(pickerRootPath);
        prop.setPickerPathIsRelative(jcrPath.isRelative());
        prop.setPickerRemembersLastVisited(jcrPath.isPickerRemembersLastVisited());
        prop.setPickerSelectableNodeTypes(jcrPath.getPickerSelectableNodeTypes());
    }

    /**
     * Order parameters according to Field Group layout, i.e. put them in order they're specified in FieldGroup definition
     */
    private static List<ContainerItemComponentPropertyRepresentation> orderParametersByFieldGroup(
            final HstComponentConfiguration componentReference,
            final Map<String, ContainerItemComponentPropertyRepresentation> propertyMap,
            final ResourceBundle[] resourceBundles) {

        final Map<String, ContainerItemComponentPropertyRepresentation> itemsMap = new LinkedHashMap<>();
        for (final DynamicFieldGroup fieldGroup : componentReference.getFieldGroups()) {
            for (final String parameterName : fieldGroup.getParameters()) {
                final ContainerItemComponentPropertyRepresentation item = propertyMap.get(parameterName);
                if (item != null) {
                    final String titleKey = fieldGroup.getTitleKey();
                    item.setGroupLabel(getResourceValue(resourceBundles, titleKey, titleKey));
                    itemsMap.put(parameterName, item);
                }
            }
        }

        propertyMap.entrySet().stream()
                .filter(entry -> !itemsMap.containsKey(entry.getKey()))
                .map(Map.Entry::getValue).forEach(v -> itemsMap.put(v.getName(), v));

        return Lists.newArrayList(itemsMap.values());
    }

    private static String getResourceValue(final ResourceBundle[] bundles, final String key, 
                                           final String defaultValue) {
        for (final ResourceBundle bundle : bundles) {
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return defaultValue;
    }

    private static String getResourceSubValue(final ResourceBundle[] bundles, final String key, final String subValue) {
        String resourceKey = key + "#" + subValue;
        for (final ResourceBundle bundle : bundles) {
            if (bundle.containsKey(resourceKey)) {
                final String value = bundle.getString(resourceKey);
                log.trace("Found translation in repository resource bundle: {} --> {}", resourceKey, value);
                return value;
            }
        }

        resourceKey = key + "/" + subValue;
        for (final ResourceBundle bundle : bundles) {
            if (bundle.containsKey(resourceKey)) {
                final String value = bundle.getString(resourceKey);
                log.trace("Found translation in Java resource bundle: {} --> {}", resourceKey, value);
                return value;
            }
        }

        log.trace("Did not find translation for key '{}' and sub value '{}', using sub value", key, subValue);
        return subValue;
    }

    /**
     * @return the ResourceBundles for <code><parameterInfo.type()</code> including the bundles for the super interfaces
     * for <code><parameterInfo.type()</code> and <code>locale</code>. The resource bundles are ordered according the 
     * interface hierarchy BREADTH FIRST traversal. Empty array if there are no resource bundles at all
     */
    protected static final ResourceBundle[] getResourceBundles(final ParametersInfo parameterInfo, 
                                                               final Locale locale) {
        
        final List<ResourceBundle> resourceBundles = new ArrayList<>();

        final List<Class<?>> breadthFirstInterfaceHierarchy = getBreadthFirstInterfaceHierarchy(parameterInfo.type());
        for (final Class<?> clazz : breadthFirstInterfaceHierarchy) {
            final ResourceBundle bundle = getResourceBundle(clazz, locale);
            if (bundle != null) {
                resourceBundles.add(bundle);
            }
        }
        return resourceBundles.toArray(new ResourceBundle[resourceBundles.size()]);
    }

    /**
     * @return the ResourceBundles for Parameter Info Class including the bundles for the super interfaces
     * for <code><parameterInfo.type()</code> and <code>locale</code>. The resource bundles are ordered according the
     * interface hierarchy BREADTH FIRST traversal. Empty array if there are no resource bundles at all
     */
    protected static final ResourceBundle[] getResourceBundles(final Class<?> componentClass, final Locale locale) {

        final List<ResourceBundle> resourceBundles = new ArrayList<>();

        final List<Class<?>> breadthFirstInterfaceHierarchy = getBreadthFirstInterfaceHierarchy(componentClass);
        for (final Class<?> clazz : breadthFirstInterfaceHierarchy) {
            final ResourceBundle bundle = getResourceBundle(clazz, locale);
            if (bundle != null) {
                resourceBundles.add(bundle);
            }
        }
        return resourceBundles.toArray(new ResourceBundle[resourceBundles.size()]);
    }

    /**
     * @return resource bundles for Parameter Info Class including the bundles for the super interfaces
     * for <code><parameterInfo.type()</code> and resource bundle document defined for the relevant catalog item
     * with the given <code>locale</code>. The resource bundles are ordered according the
     * interface hierarchy BREADTH FIRST traversal and then lastly the resource bundle documents are added if found any.
     * Empty array if there are no resource bundles at all
     */
    protected static final ResourceBundle[] getResourceBundles(final Class<?> componentClass, final Locale locale,
            final HstComponentConfiguration componentReference) {

        final List<ResourceBundle> resourceBundles = new ArrayList<>();

        final List<Class<?>> breadthFirstInterfaceHierarchy = getBreadthFirstInterfaceHierarchy(componentClass);
        for (final Class<?> clazz : breadthFirstInterfaceHierarchy) {
            final ResourceBundle bundle = getResourceBundle(clazz, locale);
            if (bundle != null) {
                resourceBundles.add(bundle);
            }
        }
        if (componentReference != null && componentReference.getComponentDefinition() != null) {
            final String resourceKey = StringUtils.replace(componentReference.getComponentDefinition(), "/", ".");

            final ResourceBundle bundle = getResourceBundle(StringUtils.substringAfter(resourceKey, "."), locale);
            if (bundle != null) {
                resourceBundles.add(bundle);
            }

        }
        return resourceBundles.toArray(new ResourceBundle[resourceBundles.size()]);
    }

    static List<Class<?>> getBreadthFirstInterfaceHierarchy(final Class<?> clazz) {
        final List<Class<?>> interfaceHierarchyList = new ArrayList<>();
        interfaceHierarchyList.add(clazz);
        populateBreadthFirstSuperInterfaces(clazz.getInterfaces(), interfaceHierarchyList);
        return interfaceHierarchyList;
    }

    private static void populateBreadthFirstSuperInterfaces(final Class<?>[] interfaces, 
                                                            final List<Class<?>> populatedSuperInterfaces) {
        
        populatedSuperInterfaces.addAll(Arrays.asList(interfaces));
        final List<Class<?>> superInterfaces = new ArrayList<>();
        for (final Class<?> clazz : interfaces) {
            superInterfaces.addAll(Arrays.asList(clazz.getInterfaces()));
        }
        if (superInterfaces.size() == 0) {
            return;
        }
        populateBreadthFirstSuperInterfaces(superInterfaces.toArray(new Class[superInterfaces.size()]), 
                populatedSuperInterfaces);
    }

    /**
     * @return the ResourceBundle for <code><parameterInfo.type()</code> and <code>locale</code> or <code>null</code>
     * when it cannot be loaded
     */
    protected static final ResourceBundle getResourceBundle(final ParametersInfo parameterInfo, final Locale locale) {
        return getResourceBundle(parameterInfo.type().getName(), locale);
    }

    private static ResourceBundle getResourceBundle(final String typeName, final Locale locale) {
        final Locale localeOrDefault;
        if (locale == null) {
            localeOrDefault = Locale.getDefault();
        } else {
            localeOrDefault = locale;
        }
        final CacheKey bundleKey = new CacheKey(typeName, localeOrDefault);
        if (FAILED_BUNDLES_TO_LOAD.contains(bundleKey)) {
            return null;
        }

        final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);
        if (localizationService != null) {
            final String bundleName = COMPONENT_PARAMETERS_TRANSLATION_LOCATION + "." + typeName;
            final org.onehippo.repository.l10n.ResourceBundle repositoryResourceBundle =
                    localizationService.getResourceBundle(bundleName, localeOrDefault);
            if (repositoryResourceBundle != null) {
                return repositoryResourceBundle.toJavaResourceBundle();
            }
        }

        try {
            return ResourceBundle.getBundle(typeName, localeOrDefault);
        } catch (final MissingResourceException e) {
            log.info("Could not find a resource bundle for class '{}', locale '{}'. The template composer properties " +
                    "panel will show displayName values instead of internationalized labels.", typeName, locale);
            FAILED_BUNDLES_TO_LOAD.add(bundleKey);
            return null;
        }
    }

    public static ResourceBundle getResourceBundle(final Class<?> clazz, final Locale locale) {
        return getResourceBundle(clazz.getName(), locale);
    }

    private static class CacheKey {
        private final String type;
        private final Locale locale;

        private CacheKey(final String type, final Locale locale) {
            if (type == null || locale == null) {
                throw new IllegalArgumentException("Both type and locale are not allowed to be null");
            }
            this.type = type;
            this.locale = locale;
        }

        @Override
        public int hashCode() {
            return type.hashCode() * 7 + locale.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof CacheKey) {
                final CacheKey other = (CacheKey) obj;
                return other.type.equals(type) && other.locale.equals(locale);
            }
            return false;
        }
    }
}

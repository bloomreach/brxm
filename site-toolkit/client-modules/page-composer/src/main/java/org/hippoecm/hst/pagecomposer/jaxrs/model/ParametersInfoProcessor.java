/*
 *  Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Session;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.hippoecm.hst.util.WebResourceUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME;

public class ParametersInfoProcessor {

    static final Logger log = LoggerFactory.getLogger(ParametersInfoProcessor.class);
    private final static  String SWITCH_TEMPLATE_I18N_KEY = "switch.template";
    private final static String CHOOSE_TEMPLATE_I18N_KEY = "choose.template";
    private final static String MISSING_TEMPLATE_I18N_KEY = "missing.template";

    private final static Set<CacheKey> failedBundlesToLoad = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private enum TemplateParamWebResource {
        NOT_CONFIGURED,
        CONFIGURED_AND_EXISTS,
        CONFIGURED_BUT_NON_EXISTING
    }

    public static List<ContainerItemComponentPropertyRepresentation> getProperties(ParametersInfo parameterInfo, Locale locale, String contentPath) {
        final ResourceBundle[] resourceBundles = getResourceBundles(parameterInfo, locale);
        final Class<?> classType = parameterInfo.type();
        if (classType == null) {
            return Collections.emptyList();
        }
        final Map<String, ContainerItemComponentPropertyRepresentation> propertyMap = createPropertyMap(contentPath, classType, resourceBundles);
        return orderPropertiesByFieldGroup(classType, resourceBundles, propertyMap);

    }

    public static List<ContainerItemComponentPropertyRepresentation> getPopulatedProperties(
            final ParametersInfo parametersInfo,
             final Locale locale,
             final String contentPath,
             final String prefix,
             final Node containerItemNode,
             final ContainerItemHelper containerItemHelper) throws RepositoryException {

        List<ContainerItemComponentPropertyRepresentation> properties = getProperties(parametersInfo, locale, contentPath);


        HstComponentParameters componentParameters = new HstComponentParameters(containerItemNode, containerItemHelper);
        if (componentParameters.hasPrefix(prefix)) {
            for (ContainerItemComponentPropertyRepresentation prop : properties) {
                String value = componentParameters.getValue(prefix, prop.getName());
                if (value != null && !value.isEmpty()) {
                    prop.setValue(value);
                }
            }
        }

        try {
            final HstComponentConfiguration componentConfiguration = containerItemHelper.getConfigObject(containerItemNode.getIdentifier());
            if (hasWebResourceFreeMarkerTemplate(componentConfiguration)) {
                // if there are multiple templates available, we inject a switchTemplateComponentPropertyRepresentation
                // containing the possible values.

                // READ I18N files from REPOSITORY and NOT from filesystem because of future 'hot web resource replacing'

                final String templateFreeMarkerPath = WebResourceUtils.webResourcePathToJcrPath(componentConfiguration.getRenderPath());

                final Session session = containerItemNode.getSession();
                if (!session.nodeExists(templateFreeMarkerPath)) {
                    String msg = String.format("Cannot find the default template '%s' for component '%s' hence" +
                            " cannot populate variants.", templateFreeMarkerPath, containerItemNode.getPath());
                    throw new IllegalStateException(msg);
                }
                final String freeMarkerVariantsFolderPath = templateFreeMarkerPath.substring(0, templateFreeMarkerPath.length() - 4);

                final List<String> variantWebResourcePaths = new ArrayList<>();
                // add the main template
                final String webResourceTemplateFreeMarkerPath = WebResourceUtils.jcrPathToWebResourcePath(templateFreeMarkerPath);
                variantWebResourcePaths.add(webResourceTemplateFreeMarkerPath);

                if (session.nodeExists(freeMarkerVariantsFolderPath)) {
                    log.debug("For freemarker '{}' there is a variants folder available. Checking variants.", templateFreeMarkerPath);

                    // check available variants
                    final Node mainTemplateFolder = session.getNode(freeMarkerVariantsFolderPath);
                    for (Node variant : new NodeIterable(mainTemplateFolder.getNodes())) {
                        if (variant.getPath().endsWith(".ftl")) {
                            log.debug("Found variant '{}' for '{}'", variant.getPath(), templateFreeMarkerPath);
                            final String variantJcrPath = variant.getPath();
                            final String variantWebResourcePath = WebResourceUtils.jcrPathToWebResourcePath(variantJcrPath);
                            variantWebResourcePaths.add(variantWebResourcePath);
                        } else {
                            log.debug("Found node '{}' below '{}' but it does not end with .ftl and is thus not a variant",
                                    variant.getPath(), freeMarkerVariantsFolderPath);
                        }
                    }
                }

                final TemplateParamWebResource templateParamWebResource;
                String templateParamValue = null;
                if (componentParameters.hasPrefix(prefix)) {
                    templateParamValue = componentParameters.getValue(prefix, TEMPLATE_PARAM_NAME);
                    if (variantWebResourcePaths.contains(templateParamValue)) {
                        templateParamWebResource = TemplateParamWebResource.CONFIGURED_AND_EXISTS;
                    } else if (StringUtils.isNotEmpty(templateParamValue)) {
                        log.info("There exists a param '{}' pointing to a non existing web resource '{}'. Setting " +
                                "value for '{}' to null", TEMPLATE_PARAM_NAME, templateParamValue, TEMPLATE_PARAM_NAME);
                        templateParamWebResource = TemplateParamWebResource.CONFIGURED_BUT_NON_EXISTING;
                    } else {
                        templateParamWebResource = TemplateParamWebResource.NOT_CONFIGURED;
                    }
                } else {
                    templateParamWebResource = TemplateParamWebResource.NOT_CONFIGURED;
                }

                if (variantWebResourcePaths.size() > 1 || templateParamWebResource == TemplateParamWebResource.CONFIGURED_BUT_NON_EXISTING) {
                    // add switch template property representation and populate the values
                    final ResourceBundle switchTemplateResourceBundle = getResourceBundle(ParametersInfoProcessor.class, locale);
                    final ResourceBundle variantsResourceBundle = loadTemplateVariantsResourceBundle(session, freeMarkerVariantsFolderPath, locale);
                    final ContainerItemComponentPropertyRepresentation switchTemplateComponentProperty =
                            createSwitchTemplateComponentPropertyRepresentation(switchTemplateResourceBundle,
                                    webResourceTemplateFreeMarkerPath, variantWebResourcePaths, variantsResourceBundle);
                    switchTemplateComponentProperty.setValue(templateParamValue);
                    if (templateParamWebResource == TemplateParamWebResource.CONFIGURED_BUT_NON_EXISTING) {
                        addMissingTemplateValueAndLabel(templateParamValue, switchTemplateResourceBundle, switchTemplateComponentProperty, variantsResourceBundle);
                    }
                    // insert the switch template on top of all the properties:
                    properties.add(0, switchTemplateComponentProperty);
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Could not populate 'switch template' property for '{}' : ", containerItemNode.getPath(), e);
            } else {
                log.warn("Could not populate 'switch template' property for '{}' : {}", containerItemNode.getPath(), e.toString());
            }
        }
        return properties;
    }

    private static boolean hasWebResourceFreeMarkerTemplate(final HstComponentConfiguration componentConfiguration) {
        final String renderPath = componentConfiguration.getRenderPath();
        if (renderPath == null) {
            return false;
        }
        return renderPath.startsWith(ContainerConstants.FREEMARKER_WEBRESOURCE_TEMPLATE_PROTOCOL) &&
                renderPath.endsWith(".ftl");
    }

    private static Map<String, ContainerItemComponentPropertyRepresentation> createPropertyMap(final String contentPath, final Class<?> classType, final ResourceBundle[] resourceBundles) {
        // although below the classType.getMethods() returns methods in random order (not in jdk 6 but it does in jdk 7 which is according spec)
        // we still create a LinkedHashMap because for jdk6 this works. For jdk 7, developers must (can only) use FieldGroup annotation
        // to specify the order of the component properties
        final Map<String, ContainerItemComponentPropertyRepresentation> propertyMap = new LinkedHashMap<>();
        for (Method method : classType.getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                final Parameter propAnnotation = method.getAnnotation(Parameter.class);
                final ContainerItemComponentPropertyRepresentation prop = new ContainerItemComponentPropertyRepresentation();
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

                final Annotation annotation = ParameterType.getTypeAnnotation(method);
                if (annotation instanceof DocumentLink) {
                    // for DocumentLink we need some extra processing
                    final DocumentLink documentLink = (DocumentLink) annotation;
                    prop.setDocType(documentLink.docType());
                    prop.setDocLocation(documentLink.docLocation());
                    prop.setAllowCreation(documentLink.allowCreation());
                } else if (annotation instanceof JcrPath) {
                    // for JcrPath we need some extra processing too
                    final JcrPath jcrPath = (JcrPath) annotation;
                    prop.setPickerConfiguration(jcrPath.pickerConfiguration());
                    prop.setPickerInitialPath(jcrPath.pickerInitialPath());
                    prop.setPickerRootPath(contentPath);
                    prop.setPickerPathIsRelative(jcrPath.isRelative());
                    prop.setPickerRemembersLastVisited(jcrPath.pickerRemembersLastVisited());
                    prop.setPickerSelectableNodeTypes(jcrPath.pickerSelectableNodeTypes());
                } else if (annotation instanceof DropDownList) {
                    final DropDownList dropDownList = (DropDownList) annotation;
                    final String values[] = dropDownList.value();
                    final String[] displayValues = new String[values.length];

                    for (int i = 0; i < values.length; i++) {
                        final String resourceKey = propAnnotation.name() + "/" + values[i];
                        displayValues[i] = getResourceValue(resourceBundles, resourceKey, values[i]);
                    }

                    prop.setDropDownListValues(values);
                    prop.setDropDownListDisplayValues(displayValues);
                }

                final ParameterType type = ParameterType.getType(method, annotation);
                prop.setType(type);

                propertyMap.put(prop.getName(), prop);
            }
        }
        return propertyMap;
    }

    private static List<ContainerItemComponentPropertyRepresentation> orderPropertiesByFieldGroup(final Class<?> classType,
                                                                                           final ResourceBundle[] resourceBundles,
                                                                                           final Map<String, ContainerItemComponentPropertyRepresentation> propertyMap) {


        // LinkedHashMultimap is a insertion ordered map that does not allow duplicate key-value entries
        Multimap<String, ContainerItemComponentPropertyRepresentation> fieldGroupProperties = LinkedHashMultimap.create();

        for (Class<?> interfaceClass : getBreadthFirstInterfaceHierarchy(classType)) {
            final FieldGroupList fieldGroupList = interfaceClass.getAnnotation(FieldGroupList.class);
            if (fieldGroupList != null) {
                FieldGroup[] fieldGroups = fieldGroupList.value();
                if (fieldGroups != null && fieldGroups.length > 0) {
                    for (FieldGroup fieldGroup : fieldGroups) {
                        final String titleKey = fieldGroup.titleKey();
                        final String groupLabel = getResourceValue(resourceBundles, titleKey, titleKey);
                        if (fieldGroup.value().length == 0) {
                            // store place holder for group
                            fieldGroupProperties.put(titleKey, null);
                        }
                        for (final String propertyName : fieldGroup.value()) {
                            final ContainerItemComponentPropertyRepresentation property = propertyMap.get(propertyName);
                            if (property == null) {
                                log.warn("Ignoring unknown parameter '{}' in parameters info interface '{}'",
                                        propertyName, classType.getCanonicalName());
                            } else if (fieldGroupProperties.containsValue(property)) {
                                log.warn("Ignoring duplicate parameter '{}' in field group '{}' of parameters info interface '{}'",
                                        new Object[]{ propertyName, fieldGroup.titleKey(), classType.getCanonicalName() });
                            } else {
                                property.setGroupLabel(groupLabel);
                                fieldGroupProperties.put(titleKey, property);
                            }
                        }
                    }
                }
            }
        }

        List<ContainerItemComponentPropertyRepresentation> orderedByFieldGroupProperties = new ArrayList<>();

        for (String titleKey : fieldGroupProperties.keySet()) {
            for (ContainerItemComponentPropertyRepresentation property : fieldGroupProperties.get(titleKey)) {
                if (property == null) {
                    // can happen due to place holder for group
                    continue;
                }
                orderedByFieldGroupProperties.add(property);
            }
        }

        for (ContainerItemComponentPropertyRepresentation property : propertyMap.values()) {
            if (orderedByFieldGroupProperties.contains(property)) {
                continue;
            }
            orderedByFieldGroupProperties.add(property);
        }

        return orderedByFieldGroupProperties;
    }

    /**
     * @return {@link ResourceBundle} and <code>null</code> when there is no jcr node at
     * <code>freeMarkerVariantsFolderPath + ".properties"</code>
     */
    private static ResourceBundle loadTemplateVariantsResourceBundle(final Session session,
                                                                     final String freeMarkerVariantsFolderPath,
                                                                     final Locale locale) throws RepositoryException {
        final String baseJcrAbsFilePath = freeMarkerVariantsFolderPath + ".properties";
        try {
            if (!session.nodeExists(baseJcrAbsFilePath)) {
                log.debug("No i18n resource bundles present for '{}'. Return null.", baseJcrAbsFilePath);
                return null;
            }
            return ResourceBundleUtils.getBundle(session, baseJcrAbsFilePath, locale);
        } catch (IllegalStateException | IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.warn("Cannot load repository based resource bundle for '{}' and locale '{}'", baseJcrAbsFilePath,
                        locale, e);
            } else {
                log.warn("Cannot load repository based resource bundle for '{}' and locale '{}' : {}", baseJcrAbsFilePath,
                        locale, e.toString());
            }
        }

        return null;
    }

    private static String getResourceValue(ResourceBundle[] bundles, String key, String defaultValue) {
        for (ResourceBundle bundle : bundles) {
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return defaultValue;
    }

    private static ContainerItemComponentPropertyRepresentation
            createSwitchTemplateComponentPropertyRepresentation(final ResourceBundle switchTemplateResourceBundle,
                                                                final String defaultTemplatePath,
                                                                final List<String> variantWebResourcePaths,
                                                                final ResourceBundle variantsResourceBundle) {

        final ContainerItemComponentPropertyRepresentation prop = new ContainerItemComponentPropertyRepresentation();
        prop.setName(TEMPLATE_PARAM_NAME);
        prop.setDefaultValue(defaultTemplatePath);
        prop.setLabel(switchTemplateResourceBundle.getString(SWITCH_TEMPLATE_I18N_KEY));
        prop.setType(ParameterType.VALUE_FROM_LIST);
        prop.setGroupLabel(switchTemplateResourceBundle.getString(CHOOSE_TEMPLATE_I18N_KEY));

        final String[] dropDownValues = variantWebResourcePaths.toArray(new String[variantWebResourcePaths.size()]);
        prop.setDropDownListValues(dropDownValues);

        String[] displayValues = new String[dropDownValues.length];
        for (int i = 0; i < dropDownValues.length; i++) {
            String variantName = StringUtils.substringAfterLast(dropDownValues[i], "/");
            if (variantsResourceBundle != null && variantsResourceBundle.containsKey(variantName)) {
                displayValues[i] = variantsResourceBundle.getString(variantName);
            } else {
                displayValues[i] = variantName;
            }
        }

        prop.setDropDownListDisplayValues(displayValues);

        return prop;
    }

    private static void addMissingTemplateValueAndLabel(final String templateParamValue, final ResourceBundle switchTemplateResourceBundle,
                                                        final ContainerItemComponentPropertyRepresentation switchTemplateComponentProperty,
                                                        final ResourceBundle variantsResourceBundle) {
        final String[] dropDownListValues = switchTemplateComponentProperty.getDropDownListValues();
        final String[] dropDownListDisplayValues = switchTemplateComponentProperty.getDropDownListDisplayValues();
        if (!StringUtils.isEmpty(templateParamValue)) {

            final String templateFileName = StringUtils.substringAfterLast(templateParamValue, "/");

            final String i18nTemplateFileName ;
            if (variantsResourceBundle != null && variantsResourceBundle.containsKey(templateFileName)) {
                i18nTemplateFileName = variantsResourceBundle.getString(templateFileName);
            } else {
                i18nTemplateFileName = templateFileName;
            }

            final String displayValue = String.format(switchTemplateResourceBundle.getString(MISSING_TEMPLATE_I18N_KEY),
                    i18nTemplateFileName);
            final String[] augmentedDropDownListValues = new String[dropDownListValues.length + 1];
            final String[] augmentedDropDownListDisplayValues = new String[dropDownListValues.length + 1];
            augmentedDropDownListValues[0] = templateParamValue;
            System.arraycopy(dropDownListValues, 0, augmentedDropDownListValues, 1, dropDownListValues.length);

            augmentedDropDownListDisplayValues[0] = displayValue;
            System.arraycopy(dropDownListDisplayValues, 0, augmentedDropDownListDisplayValues, 1, dropDownListDisplayValues.length);
            switchTemplateComponentProperty.setDropDownListValues(augmentedDropDownListValues);
            switchTemplateComponentProperty.setDropDownListDisplayValues(augmentedDropDownListDisplayValues);

        }
    }

    /**
     *
     * @return the ResourceBundles for <code><parameterInfo.type()</code> including the bundles for the super interfaces for
     * <code><parameterInfo.type()</code> and <code>locale</code>. The resource bundles are ordered according the interface
     * hierarchy BREADTH FIRST traversal. Empty array if there are no resource bundles at all
     */
    protected static final ResourceBundle[] getResourceBundles(final ParametersInfo parameterInfo, final Locale locale) {
        List<ResourceBundle> resourceBundles = new ArrayList<ResourceBundle>();

        final List<Class<?>> breadthFirstInterfaceHierarchy = getBreadthFirstInterfaceHierarchy(parameterInfo.type());
        for (Class<?> clazz : breadthFirstInterfaceHierarchy) {
            ResourceBundle bundle = getResourceBundle(clazz, locale);
            if (bundle != null) {
                resourceBundles.add(bundle);
            }
        }
        return resourceBundles.toArray(new ResourceBundle[resourceBundles.size()]);
    }

    static List<Class<?>> getBreadthFirstInterfaceHierarchy(final Class<?> clazz) {
        final List<Class<?>> interfaceHierarchList = new ArrayList<>();
        interfaceHierarchList.add(clazz);
        populateBreadthFirstSuperInterfaces(clazz.getInterfaces(), interfaceHierarchList);
        return interfaceHierarchList;
    }

    private static void populateBreadthFirstSuperInterfaces(final Class<?>[] interfaces, final List<Class<?>> populatedSuperInterfaces) {
        for (Class<?> clazz : interfaces) {
            populatedSuperInterfaces.add(clazz);
        }
        List<Class<?>> superInterfaces = new ArrayList<>();
        for (Class<?> clazz : interfaces) {
            superInterfaces.addAll(Arrays.asList(clazz.getInterfaces()));
        }
        if (superInterfaces.size() == 0) {
            return;
        }
        populateBreadthFirstSuperInterfaces(superInterfaces.toArray(new Class[superInterfaces.size()]), populatedSuperInterfaces);
    }

    /**
     * @return the ResourceBundle for <code><parameterInfo.type()</code> and <code>locale</code> or <code>null</code> when
     * it cannot be loaded
     */
    protected static final ResourceBundle getResourceBundle(final ParametersInfo parameterInfo, final Locale locale) {
        return getResourceBundle(parameterInfo.type(), locale);
    }

    static ResourceBundle getResourceBundle(final Class<?> clazz, final Locale locale) {
        final Locale localeOrDefault;
        if (locale == null) {
            localeOrDefault = Locale.getDefault();
        } else {
            localeOrDefault = locale;
        }
        final String typeName = clazz.getName();
        CacheKey bundleKey = new CacheKey(typeName, localeOrDefault);
        if (failedBundlesToLoad.contains(bundleKey)) {
            return null;
        }
        try {
            return ResourceBundle.getBundle(typeName, localeOrDefault);
        } catch (MissingResourceException e) {
            log.info("Could not find a resource bundle for class '{}', locale '{}'. The template composer " +
                    "properties panel will show displayName values instead of internationalized labels.", typeName, locale);
            failedBundlesToLoad.add(bundleKey);
            return null;
        }
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
                CacheKey other = (CacheKey) obj;
                return other.type.equals(type) && other.locale.equals(locale);
            }
            return false;
        }
    }

}

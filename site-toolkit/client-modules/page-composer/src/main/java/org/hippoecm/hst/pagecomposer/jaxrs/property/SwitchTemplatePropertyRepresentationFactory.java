/*
 *  Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.property;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ParameterType;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.AbstractHstComponentParameters;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.hippoecm.hst.util.WebFileUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils.getEditingPreviewVirtualHosts;

public class SwitchTemplatePropertyRepresentationFactory implements PropertyRepresentationFactory {

    private static final Logger log = LoggerFactory.getLogger(SwitchTemplatePropertyRepresentationFactory.class);

    private final String SWITCH_TEMPLATE_RESOURCE_BUNDLE = "hippo:channelmanager.switch-template";

    private final static String SWITCH_TEMPLATE_I18N_KEY = "switch.template";
    private final static String CHOOSE_TEMPLATE_I18N_KEY = "choose.template";
    private final static String MISSING_TEMPLATE_I18N_KEY = "missing.template";

    private static final String FTL_EXTENSION = ".ftl";

    private Set<String> templateExtensions = unmodifiableSet(singleton(FTL_EXTENSION));
    private enum TemplateParamWebFile {
        NOT_CONFIGURED,
        CONFIGURED_AND_EXISTS,
        CONFIGURED_BUT_NON_EXISTING
    }

    public static class TemplateDisplayNameComparator implements Comparator<String> {
        private Set<String> extensions;

        public TemplateDisplayNameComparator(final Set<String> extensions) {
            this.extensions = extensions;
        }

        @Override
        public int compare(final String key1, final String key2) {

            // no null check, if key1 or key2 is null, just NPE

            final String compare1;
            final boolean key1HasExtensionSuffix;

            final String extension = getExtension(key1);
            if (extension != null && key1.endsWith(extension)) {
                compare1 = key1.substring(0, key1.length() - extension.length());
                key1HasExtensionSuffix = true;
            } else {
                compare1 = key1;
                key1HasExtensionSuffix = false;
            }
            final String compare2;
            final boolean key2HasExtensionSuffix;
            final String extension2 = getExtension(key2);

            if (extension2 != null && key2.endsWith(extension2)) {
                compare2 = key2.substring(0, key2.length() - extension2.length());
                key2HasExtensionSuffix = true;
            } else {
                compare2 = key2;
                key2HasExtensionSuffix = false;
            }

            int compare = compare1.compareTo(compare2);
            if (compare != 0) {
                return compare;
            }

            if (key1HasExtensionSuffix && key2HasExtensionSuffix) {
                // also sort on suffix, in case of mixed extensions
                return extension.compareTo(extension2);
            }

            if (key1HasExtensionSuffix) {
                return 1;
            }
            return -1;
        }

        private String getExtension(final String key) {
            if (key != null && key.indexOf('.') != -1) {
                final String extension = key.substring(key.lastIndexOf('.'));
                if (extensions.contains(extension)) {
                    return extension;
                }
            }
            return null;
        }
    }

    public Set<String> getTemplateExtensions() {
        return templateExtensions;
    }

    public void setTemplateExtensions(final Set<String> templateExtensions) {
        this.templateExtensions = unmodifiableSet(templateExtensions);
    }

    @Override
    public ContainerItemComponentPropertyRepresentation createProperty(final Locale locale,
                                                                       final String contentPath,
                                                                       final String prefix,
                                                                       final Node containerItemNode,
                                                                       final HstComponentConfiguration componentConfiguration,
                                                                       final AbstractHstComponentParameters componentParameters,
                                                                       final List<ContainerItemComponentPropertyRepresentation> properties) {
        String containerItemPath = null;
        try {
            containerItemPath = containerItemNode.getPath();

            if (hasWebFileTemplate(componentConfiguration, getTemplateExtensions())) {
                // if there are multiple templates available, we inject a switchTemplateComponentPropertyRepresentation
                // containing the possible values.

                // READ I18N files from REPOSITORY and NOT from filesystem because of future 'hot web file replacing'
                String bundleName = getEditingPreviewVirtualHosts().getContextPath();
                if (!bundleName.isEmpty()) {
                    bundleName = bundleName.substring(1);
                }
                final String renderPath = componentConfiguration.getRenderPath();
                final int idx = renderPath.substring(renderPath.lastIndexOf('.')).length();
                final String templatePath = WebFileUtils.webFilePathToJcrPath(renderPath, bundleName);

                final Session session = containerItemNode.getSession();
                if (!session.nodeExists(templatePath)) {
                    String msg = String.format("Cannot find the default template '%s' for component '%s' hence" +
                            " cannot populate variants.", templatePath, containerItemPath);
                    throw new IllegalStateException(msg);
                }
                final String variantsFolderPath = templatePath.substring(0, templatePath.length() - idx);

                final List<String> variantWebFilePaths = new ArrayList<>();
                // add the main template
                final String webFileTemplatePath = WebFileUtils.jcrPathToWebFilePath(templatePath, bundleName);
                variantWebFilePaths.add(webFileTemplatePath);

                if (session.nodeExists(variantsFolderPath)) {
                    log.debug("For template '{}' there is a variants folder available. Checking variants.", templatePath);

                    // check available variants
                    final Node mainTemplateFolder = session.getNode(variantsFolderPath);
                    for (Node variant : new NodeIterable(mainTemplateFolder.getNodes())) {
                        final String path = variant.getPath();
                        if (validExtension(path, getTemplateExtensions())) {
                            log.debug("Found variant '{}' for '{}'", path, templatePath);
                            final String variantWebFilePath = WebFileUtils.jcrPathToWebFilePath(path, bundleName);
                            variantWebFilePaths.add(variantWebFilePath);
                        } else {
                            log.debug("Found node '{}' below '{}' but it does not end with one of the configured extensions {} and is thus not a variant",
                                    path, variantsFolderPath, getTemplateExtensions());
                        }
                    }
                }

                final TemplateParamWebFile templateParamWebFile;
                String templateParamValue = null;
                if (componentParameters.hasPrefix(prefix)) {
                    templateParamValue = componentParameters.getValue(prefix, TEMPLATE_PARAM_NAME);
                    if (variantWebFilePaths.contains(templateParamValue)) {
                        templateParamWebFile = TemplateParamWebFile.CONFIGURED_AND_EXISTS;
                    } else if (StringUtils.isNotEmpty(templateParamValue)) {
                        log.info("There exists a param '{}' pointing to a non existing web file '{}'. Setting " +
                                "value for '{}' to null", TEMPLATE_PARAM_NAME, templateParamValue, TEMPLATE_PARAM_NAME);
                        templateParamWebFile = TemplateParamWebFile.CONFIGURED_BUT_NON_EXISTING;
                    } else {
                        templateParamWebFile = TemplateParamWebFile.NOT_CONFIGURED;
                    }
                } else {
                    templateParamWebFile = TemplateParamWebFile.NOT_CONFIGURED;
                }

                if (variantWebFilePaths.size() > 1 || templateParamWebFile == TemplateParamWebFile.CONFIGURED_BUT_NON_EXISTING) {
                    // add switch template property representation and populate the values
                    final ResourceBundle switchTemplateResourceBundle = loadSwitchTemplateResourceBundle(locale);
                    final ResourceBundle variantsResourceBundle = loadTemplateVariantsResourceBundle(session, variantsFolderPath, locale);
                    final ContainerItemComponentPropertyRepresentation switchTemplateComponentProperty =
                            createSwitchTemplateComponentPropertyRepresentation(switchTemplateResourceBundle,
                                    webFileTemplatePath, variantWebFilePaths, variantsResourceBundle);
                    switchTemplateComponentProperty.setValue(templateParamValue);

                    // the addMissingTemplateValueAndLabel is added *after* sorting since always most be on top
                    sortDropDownByDisplayValue(switchTemplateComponentProperty, getTemplateExtensions());

                    if (templateParamWebFile == TemplateParamWebFile.CONFIGURED_BUT_NON_EXISTING) {
                        addMissingTemplateValueAndLabel(templateParamValue, switchTemplateResourceBundle, switchTemplateComponentProperty, variantsResourceBundle);
                    }

                    return switchTemplateComponentProperty;
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Could not populate 'switch template' property for '{}' : ", containerItemPath, e);
            } else {
                log.warn("Could not populate 'switch template' property for '{}' : {}", containerItemPath, e.toString());
            }
        }
        return null;
    }

    private ResourceBundle loadSwitchTemplateResourceBundle(final Locale locale) {
        final Locale localeOrDefault;
        if (locale == null) {
            localeOrDefault = LocalizationService.DEFAULT_LOCALE;
        } else {
            localeOrDefault = locale;
        }

        final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);
        if (localizationService != null) {
            final org.onehippo.repository.l10n.ResourceBundle repositoryResourceBundle =
                    localizationService.getResourceBundle(SWITCH_TEMPLATE_RESOURCE_BUNDLE, localeOrDefault);
            if (repositoryResourceBundle != null) {
                return repositoryResourceBundle.toJavaResourceBundle();
            }
        }

        log.warn("Could not load switch template resource bundle");

        // the fallback to property files is for unit tests only
        try {
            return ResourceBundle.getBundle(SwitchTemplatePropertyRepresentationFactory.class.getName(), localeOrDefault);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    private static boolean hasWebFileTemplate(final HstComponentConfiguration componentConfiguration, final Set<String> templateExtensions) {
        final String renderPath = componentConfiguration.getRenderPath();
        if (renderPath == null) {
            return false;
        }
        return renderPath.startsWith(ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL) && validExtension(renderPath, templateExtensions);
    }

    /**
     * @return {@link ResourceBundle} and <code>null</code> when there is no jcr node at
     * <code>variantsFolderPath + ".properties"</code>
     */
    private static ResourceBundle loadTemplateVariantsResourceBundle(final Session session,
                                                                     final String variantsFolderPath,
                                                                     final Locale locale) throws RepositoryException {
        final String baseJcrAbsFilePath = variantsFolderPath + ".properties";
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

    private static String getResourceBundleValueOrDefault(final ResourceBundle bundle, final String key) {
        if (bundle != null) {
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return key;
    }

    private static ContainerItemComponentPropertyRepresentation createSwitchTemplateComponentPropertyRepresentation(
            final ResourceBundle switchTemplateResourceBundle,
            final String defaultTemplatePath,
            final List<String> variantWebFilePaths,
            final ResourceBundle variantsResourceBundle) {

        final ContainerItemComponentPropertyRepresentation prop = new ContainerItemComponentPropertyRepresentation();
        prop.setName(TEMPLATE_PARAM_NAME);
        prop.setDefaultValue(defaultTemplatePath);
        prop.setLabel(getResourceBundleValueOrDefault(switchTemplateResourceBundle, SWITCH_TEMPLATE_I18N_KEY));
        prop.setType(ParameterType.VALUE_FROM_LIST);
        prop.setGroupLabel(getResourceBundleValueOrDefault(switchTemplateResourceBundle, CHOOSE_TEMPLATE_I18N_KEY));

        final String[] dropDownValues = variantWebFilePaths.toArray(new String[variantWebFilePaths.size()]);
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

            final String i18nTemplateFileName;
            if (variantsResourceBundle != null && variantsResourceBundle.containsKey(templateFileName)) {
                i18nTemplateFileName = variantsResourceBundle.getString(templateFileName);
            } else {
                i18nTemplateFileName = templateFileName;
            }

            final String displayValue = MessageFormat.format(
                    getResourceBundleValueOrDefault(switchTemplateResourceBundle, MISSING_TEMPLATE_I18N_KEY),
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

    public static void sortDropDownByDisplayValue(final ContainerItemComponentPropertyRepresentation switchTemplateComponentProperty, final Set<String> extensions) {
        try {
            Map<String, String> sortedMap = asKeySortedMap(switchTemplateComponentProperty.getDropDownListDisplayValues(),
                    switchTemplateComponentProperty.getDropDownListValues(), extensions);

            switchTemplateComponentProperty.setDropDownListValues(sortedMap.values().toArray(new String[sortedMap.size()]));
            switchTemplateComponentProperty.setDropDownListDisplayValues(sortedMap.keySet().toArray(new String[sortedMap.size()]));
        } catch (IllegalArgumentException e) {
            log.warn("Could not sort map: {}", e.toString());
        }
    }


    /**
     * @param keys   array of keys to sort on and which must be of equal length as <code>values</code>
     * @param values arrays of values which must be of equal length as <code>keys</code>
     * @param extensions set of accepted extensions
     * @return A {@link java.util.Map} of the <code>keys</code> and <code>values</code> sorted on <code>keys</code>
     * @throws java.lang.IllegalArgumentException if <code>keys</code> length is not equal to <code>values</code> length
     */
    public static Map<String, String> asKeySortedMap(String[] keys, String[] values, final Set<String> extensions) {
        if (keys.length != values.length) {
            final String msg = String.format("Cannot return sorted map on keys when keys and values when arrays are of unequal length. " +
                    "Cannot sort '%s' and '%s'", Arrays.toString(keys), Arrays.toString(values));
            throw new IllegalArgumentException(msg);
        }

        final Map<String, String> sortedMap = new TreeMap<>(new TemplateDisplayNameComparator(extensions));
        for (int i = 0; i < keys.length; i++) {
            sortedMap.put(keys[i], values[i]);
        }
        return sortedMap;
    }

    private static boolean validExtension(final String path, final Set<String> templateExtensions) {
        for (String templateExtension : templateExtensions) {
            if (path.endsWith(templateExtension)) {
                return true;
            }
        }
        return false;
    }


}
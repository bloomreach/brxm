/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ParameterType;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.hippoecm.hst.util.WebResourceUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME;

public class SwitchTemplatePropertyRepresentationFactory implements PropertyRepresentationFactory {

    private static final Logger log = LoggerFactory.getLogger(SwitchTemplatePropertyRepresentationFactory.class);

    private final static  String SWITCH_TEMPLATE_I18N_KEY = "switch.template";
    private final static String CHOOSE_TEMPLATE_I18N_KEY = "choose.template";
    private final static String MISSING_TEMPLATE_I18N_KEY = "missing.template";

    private enum TemplateParamWebResource {
        NOT_CONFIGURED,
        CONFIGURED_AND_EXISTS,
        CONFIGURED_BUT_NON_EXISTING
    }

    @Override
    public ContainerItemComponentPropertyRepresentation createProperty(final ParametersInfo parametersInfo,
                                                                       final Locale locale,
                                                                       final String contentPath,
                                                                       final String prefix,
                                                                       final Node containerItemNode,
                                                                       final ContainerItemHelper containerItemHelper,
                                                                       final HstComponentParameters componentParameters,
                                                                       final List<ContainerItemComponentPropertyRepresentation> properties) {
        String containerItemPath = null;
        try {
            containerItemPath = containerItemNode.getPath();
            final HstComponentConfiguration componentConfiguration = containerItemHelper.getConfigObject(containerItemNode.getIdentifier());
            if (hasWebResourceFreeMarkerTemplate(componentConfiguration)) {
                // if there are multiple templates available, we inject a switchTemplateComponentPropertyRepresentation
                // containing the possible values.

                // READ I18N files from REPOSITORY and NOT from filesystem because of future 'hot web resource replacing'

                final String templateFreeMarkerPath = WebResourceUtils.webResourcePathToJcrPath(componentConfiguration.getRenderPath());

                final Session session = containerItemNode.getSession();
                if (!session.nodeExists(templateFreeMarkerPath)) {
                    String msg = String.format("Cannot find the default template '%s' for component '%s' hence" +
                            " cannot populate variants.", templateFreeMarkerPath, containerItemPath);
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
                    final ResourceBundle switchTemplateResourceBundle = ParametersInfoProcessor.getResourceBundle(ParametersInfoProcessor.class, locale);
                    final ResourceBundle variantsResourceBundle = loadTemplateVariantsResourceBundle(session, freeMarkerVariantsFolderPath, locale);
                    final ContainerItemComponentPropertyRepresentation switchTemplateComponentProperty =
                            createSwitchTemplateComponentPropertyRepresentation(switchTemplateResourceBundle,
                                    webResourceTemplateFreeMarkerPath, variantWebResourcePaths, variantsResourceBundle);
                    switchTemplateComponentProperty.setValue(templateParamValue);
                    if (templateParamWebResource == TemplateParamWebResource.CONFIGURED_BUT_NON_EXISTING) {
                        addMissingTemplateValueAndLabel(templateParamValue, switchTemplateResourceBundle, switchTemplateComponentProperty, variantsResourceBundle);
                    }
                    // insert the switch template on top of all the properties:
                    //properties.add(0, switchTemplateComponentProperty);
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

    private static boolean hasWebResourceFreeMarkerTemplate(final HstComponentConfiguration componentConfiguration) {
        final String renderPath = componentConfiguration.getRenderPath();
        if (renderPath == null) {
            return false;
        }
        return renderPath.startsWith(ContainerConstants.FREEMARKER_WEBRESOURCE_TEMPLATE_PROTOCOL) &&
                renderPath.endsWith(".ftl");
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
}

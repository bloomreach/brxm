/*
 *  Copyright 2010 Hippo.
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component node wrapper that will be mapped automatically by the JAXRS (Jettison or JAXB) for generating the JSON/XML
 */
@XmlRootElement(name = "component")
public class ContainerItemComponentRepresentation {
    
    private static Logger log = LoggerFactory.getLogger(ContainerItemComponentRepresentation.class);
            
    private static final String HST_PARAMETERNAMES = "hst:parameternames";
    private static final String HST_COMPONENTCLASSNAME = "hst:componentclassname";
    private static final String HST_PARAMETERVALUES = "hst:parametervalues";
    private static final String HST_PARAMETERNAMEPREFIXES = "hst:parameternameprefixes";

    private List<ContainerItemComponentPropertyRepresentation> properties;

    /**
     * For now, the supported UI xtypes (used for rendering the UI view) are 
     * <ul>
     *   <li>textfield</li>
     *   <li>numberfield</li>
     *   <li>checkbox</li>
     *   <li>datefield</li>
     *   <li>colorfield : see also {@link Color}</li>
     *   <li>combo : see also {@link DocumentLink}</li>
     * </ul>
     * The ParameterType.UNKNOWN will result in xtype "textfield"
     */
    enum ParameterType {
        STRING("textfield", new Class[]{String.class}, null),
        VALUE_FROM_LIST("combo", new Class[]{String.class}, DropDownList.class),
        NUMBER("numberfield", new Class[]{Long.class, long.class, Integer.class, int.class, Short.class, short.class}, null), 
        BOOLEAN("checkbox", new Class[]{Boolean.class, boolean.class}, null), 
        DATE("datefield", new Class[]{Date.class, Calendar.class}, null), 
        COLOR("colorfield", new Class[]{String.class}, Color.class),
        DOCUMENT("documentcombobox", new Class[]{String.class}, DocumentLink.class),
        JCR_PATH("linkpicker", new Class[]{String.class}, JcrPath.class),
        UNKNOWN("textfield", null, null);
        
        final String xtype;
        HashSet<Class<?>> supportedReturnTypes = null;
        final Class<?> annotationType;

        ParameterType(String xtype, Class<?>[] supportedReturnTypes, Class<?> annotationType) {
            this.xtype = xtype;
            if(supportedReturnTypes != null) {
                this.supportedReturnTypes = new HashSet<Class<?>>(Arrays.asList(supportedReturnTypes));
            }
            this.annotationType = annotationType;
        }
        
        private boolean supportsReturnType(Class<?> returnType) {
            if( supportedReturnTypes == null) {
                return false;
            }
            return supportedReturnTypes.contains(returnType);
        }

        static Annotation getTypeAnnotation(final Method method) {
            for (Annotation annotation : method.getAnnotations()) {
                for (ParameterType type : ParameterType.values()) {
                    if (annotation.annotationType() == type.annotationType) {
                        if (type.supportsReturnType(method.getReturnType())) {
                            return annotation;
                        } else {
                            log.warn("return type '{}' of method '{}' is not supported for annotation '{}'.", new String[]{method.getReturnType().getName(), method.getName(), type.annotationType.getName()});
                        }
                    }
                }
            }
            return null;
        }

        static ParameterType getType(final Method method, Annotation annotation) {
            if (annotation != null) {
                for (ParameterType type : ParameterType.values()) {
                    if (annotation.annotationType() == type.annotationType) {
                        return type;
                    }
                }
            }
            for (ParameterType type : ParameterType.values()) {
                if (type.supportsReturnType(method.getReturnType())) {
                    return type;
                }
            }
            return ParameterType.UNKNOWN;
        }

    }

    
    /**
     * Constructs a component node wrapper
     *
     * @param node JcrNode for a component.
     * @param locale the locale to get localized names, can be null
     * @param prefix  the parameter prefix
     * @throws RepositoryException    Thrown if the repository exception occurred during reading of the properties.
     * @throws ClassNotFoundException thrown when this class can't instantiate the component class.
     */
    public ContainerItemComponentRepresentation represents(Node node, Locale locale, String prefix, String currentMountCanonicalContentPath) throws RepositoryException, ClassNotFoundException {
        properties = new ArrayList<ContainerItemComponentPropertyRepresentation>();

        HstComponentParameters componentParameters = new HstComponentParameters(node);

        //Get the properties via annotation on the component class
        String componentClassName = null;
        if (node.hasProperty(HST_COMPONENTCLASSNAME)) {
            componentClassName = node.getProperty(HST_COMPONENTCLASSNAME).getString();
        }

        if (componentClassName != null) {
            Class<?> componentClass = Thread.currentThread().getContextClassLoader().loadClass(componentClassName);
            if (componentClass.isAnnotationPresent(ParametersInfo.class)) {
                ParametersInfo parametersInfo = (ParametersInfo) componentClass.getAnnotation(ParametersInfo.class);
                properties = getProperties(parametersInfo, locale, currentMountCanonicalContentPath);
            }
            if (componentParameters.hasPrefix(prefix)) {
                for (ContainerItemComponentPropertyRepresentation prop : properties) {
                    String value = componentParameters.getValue(prefix, prop.getName());
                    if (value != null && !value.isEmpty()) {
                        prop.setValue(value);
                    }
                }
            }
        }
        return this;
    }

    public List<ContainerItemComponentPropertyRepresentation> getProperties() {
        return properties;
    }

    public void setProperties(List<ContainerItemComponentPropertyRepresentation> properties) {
        this.properties = properties;
    }
    
    static List<ContainerItemComponentPropertyRepresentation> getProperties(ParametersInfo parameterInfo, Locale locale,
                                                                            String currentMountCanonicalContentPath) {
        final List<ContainerItemComponentPropertyRepresentation> properties = new ArrayList<ContainerItemComponentPropertyRepresentation>();

        final Class<?> classType = parameterInfo.type();
        if (classType == null) {
            return properties;
        }

        ResourceBundle resourceBundle = null;
        final String typeName = parameterInfo.type().getName();
        try {
            if (locale != null) {
                resourceBundle = ResourceBundle.getBundle(typeName, locale);
            } else {
                resourceBundle = ResourceBundle.getBundle(typeName);
            }
        } catch (MissingResourceException missingResourceException) {
            log.debug("Could not find a resource bundle for class '{}', locale '{}'. The template composer properties panel will show displayName values instead of internationalised labels.", new Object[]{typeName, locale});
        }

        for (Method method : classType.getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                final Parameter propAnnotation = method.getAnnotation(Parameter.class);
                final ContainerItemComponentPropertyRepresentation prop = new ContainerItemComponentPropertyRepresentation();
                prop.setName(propAnnotation.name());
                prop.setDefaultValue(propAnnotation.defaultValue());
                prop.setDescription(propAnnotation.description());
                prop.setRequired(propAnnotation.required());
                prop.setHiddenInChannelManager(propAnnotation.hideInChannelManager());
                if (resourceBundle != null && resourceBundle.containsKey(propAnnotation.name())) {
                    prop.setLabel(resourceBundle.getString(propAnnotation.name()));
                } else {
                    if (propAnnotation.displayName().equals("")) {
                        prop.setLabel(propAnnotation.name());
                    } else {
                        prop.setLabel(propAnnotation.displayName());
                    }
                }

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
                    prop.setPickerRootPath(currentMountCanonicalContentPath);
                    prop.setPickerPathIsRelative(jcrPath.isRelative());
                    prop.setPickerRemembersLastVisited(jcrPath.pickerRemembersLastVisited());
                    prop.setPickerSelectableNodeTypes(jcrPath.pickerSelectableNodeTypes());
                } else if (annotation instanceof DropDownList) {
                    final DropDownList dropDownList = (DropDownList)annotation;
                    final String values[] = dropDownList.value();
                    final String[] displayValues = new String[values.length];

                    for (int i = 0; i < values.length; i++) {
                        final String resourceKey = propAnnotation.name() + "/" + values[i];
                        if (resourceBundle != null && resourceBundle.containsKey(resourceKey)) {
                            displayValues[i] = resourceBundle.getString(resourceKey);
                        } else {
                            displayValues[i] = values[i];
                        }
                    }

                    prop.setDropDownListValues(values);
                    prop.setDropDownListDisplayValues(displayValues);
                }

                final ParameterType type = ParameterType.getType(method, annotation);
                prop.setType(type);

                // Set the value to be default value before setting it with original value
                properties.add(prop);
            }
        }

        return properties;
    }

}

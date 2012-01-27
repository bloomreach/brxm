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
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
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
        NUMBER("numberfield", new Class[]{Long.class, long.class, Integer.class, int.class, Short.class, short.class}, null), 
        BOOLEAN("checkbox", new Class[]{Boolean.class, boolean.class}, null), 
        DATE("datefield", new Class[]{Date.class}, null), 
        COLOR("colorfield", new Class[]{String.class}, Color.class), 
        DOCUMENT("combo", new Class[]{String.class}, DocumentLink.class), 
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
        
        static ParameterType getType(final Method method, Parameter propAnnotation) {
            // first check the annotations combined with return type. If no match from annotations, check return type only
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation == propAnnotation) {
                    continue;
                }
                for(ParameterType type : ParameterType.values()) {
                    if(annotation.annotationType() == type.annotationType) {
                        if (type.supportsReturnType(method.getReturnType())) {
                           return type;  
                        } else {
                            log.warn("return type '{}' of method '{}' is not supported for annotation '{}'.", new String[]{method.getReturnType().getName(), method.getName(), type.annotationType.getName()});
                        }
                    }
                }
            }
            
            for(ParameterType type : ParameterType.values()) {
                if(type.supportsReturnType(method.getReturnType())) {
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
    public ContainerItemComponentRepresentation represents(Node node, Locale locale, String prefix) throws RepositoryException, ClassNotFoundException {
        properties = new ArrayList<ContainerItemComponentPropertyRepresentation>();
        Map<String, String> hstParameters = null;
        Map<String, String> parameterDefaults = new HashMap<String, String>();
        //Get the parameter names and values from the component node.
        if (node.hasProperty(HST_PARAMETERNAMES) && node.hasProperty(HST_PARAMETERVALUES)) {
            hstParameters = new HashMap<String, String>();
            Value[] paramNames = node.getProperty(HST_PARAMETERNAMES).getValues();
            Value[] paramValues = node.getProperty(HST_PARAMETERVALUES).getValues();
            if (node.hasProperty(HST_PARAMETERNAMEPREFIXES)) {
                Value[] paramPrefixes = node.getProperty(HST_PARAMETERNAMEPREFIXES).getValues();
                for (int i = 0; i < paramNames.length; i++) {
                    if (paramPrefixes[i].getString().equals(prefix)) {
                        hstParameters.put(paramNames[i].getString(), paramValues[i].getString());
                    } else if (paramPrefixes[i].getString().isEmpty()) {
                        parameterDefaults.put(paramNames[i].getString(), paramValues[i].getString());
                    }
                }
            } else if (prefix == null || prefix.isEmpty()) {
                for (int i = 0; i < paramNames.length; i++) {
                    hstParameters.put(paramNames[i].getString(), paramValues[i].getString());
                }
            } else {
                for (int i = 0; i < paramNames.length; i++) {
                    parameterDefaults.put(paramNames[i].getString(), paramValues[i].getString());
                }
            }
        }

        //Get the properties via annotation on the component class
        String componentClassName = null;
        if (node.hasProperty(HST_COMPONENTCLASSNAME)) {
            componentClassName = node.getProperty(HST_COMPONENTCLASSNAME).getString();
        }

        if (componentClassName != null) {
            Class<?> componentClass = Thread.currentThread().getContextClassLoader().loadClass(componentClassName);
            if (componentClass.isAnnotationPresent(ParametersInfo.class)) {
                ParametersInfo parametersInfo = (ParametersInfo) componentClass.getAnnotation(ParametersInfo.class);
                properties = getProperties(parametersInfo, locale);
            }
            if (hstParameters != null) {
                for (ContainerItemComponentPropertyRepresentation prop : properties) {
                    String value = hstParameters.get(prop.getName());
                    if (value != null && !value.isEmpty()) {
                        prop.setValue(value);
                    }
                    String defaultValue = parameterDefaults.get(prop.getName());
                    if (defaultValue != null && !defaultValue.isEmpty()) {
                        prop.setDefaultValue(defaultValue);
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
    
    static List<ContainerItemComponentPropertyRepresentation> getProperties(ParametersInfo parameterInfo, Locale locale) {
        final List<ContainerItemComponentPropertyRepresentation> properties = new ArrayList<ContainerItemComponentPropertyRepresentation>();

        final Class<?> classType = parameterInfo.type();
        if (classType == null) {
            return properties;
        }

        ResourceBundle resourceBundle = null;
        if (locale != null) {
            try {
                resourceBundle = ResourceBundle.getBundle(parameterInfo.type().getName(), locale);
            } catch (MissingResourceException missingResourceException) {
                log.info("Could not find a resource bundle for class '{}', locale '{}'. The template composer properties panel will show displayName values instead of internationalised labels.", new Object[]{parameterInfo.type().getName(), locale});
            }
        }

        for (Method method : classType.getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                final Parameter propAnnotation = method.getAnnotation(Parameter.class);
                final ContainerItemComponentPropertyRepresentation prop = new ContainerItemComponentPropertyRepresentation();
                prop.setName(propAnnotation.name());
                prop.setDefaultValue(propAnnotation.defaultValue());
                prop.setDescription(propAnnotation.description());
                prop.setRequired(propAnnotation.required());
                if (resourceBundle != null && resourceBundle.containsKey(propAnnotation.name())) {
                    prop.setLabel(resourceBundle.getString(propAnnotation.name()));
                } else {
                    if (propAnnotation.displayName().equals("")) {
                        prop.setLabel(propAnnotation.name());
                    } else {
                        prop.setLabel(propAnnotation.displayName());
                    }
                }

                ParameterType type = ParameterType.getType(method, propAnnotation);

                if (type.annotationType == DocumentLink.class) {
                    // for DocumentLink we need some extra processing
                    for (Annotation annotation : method.getAnnotations()) {
                        if (annotation.annotationType() != type.annotationType) {
                            continue;
                        }
                        type = ContainerItemComponentRepresentation.ParameterType.DOCUMENT;
                        DocumentLink documentLink = (DocumentLink) annotation;
                        prop.setDocType(documentLink.docType());
                        prop.setDocLocation(documentLink.docLocation());
                        prop.setAllowCreation(documentLink.allowCreation());
                        break;
                    }
                }
                    
                prop.setType(type);

                // Set the value to be default value before setting it with original value
                properties.add(prop);
            }
        }

        return properties;
    }


}

/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.site.content;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.ContentTypesProvider;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.VersionedObjectConverterProxy;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.util.ClasspathResourceScanner;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.web.context.ServletContextAware;

/**
 * ObjectConverter factory bean
 */
public class ObjectConverterFactoryBean extends AbstractFactoryBean<ObjectConverter> implements ComponentManagerAware, ServletContextAware {
    private static final int GLOBAL_RESOURCE_PATH_SIZE = 3;

    private ServletContext servletContext;
    private ClasspathResourceScanner classpathResourceScanner;
    private String annotatedClassesInitParam = DefaultContentBeansTool.BEANS_ANNOTATED_CLASSES_CONF_PARAM;
    private String annotatedClassesResourcePath;
    private ContentTypesProvider contentTypesProvider;
    private Boolean generateDynamicBean;

    public Boolean getGenerateDynamicBean() {
        return generateDynamicBean;
    }

    public void setGenerateDynamicBean(Boolean generateDynamicBean) {
        this.generateDynamicBean = generateDynamicBean;
    }

    public ClasspathResourceScanner getClasspathResourceScanner() {
        return classpathResourceScanner;
    }

    public void setClasspathResourceScanner(ClasspathResourceScanner classpathResourceScanner) {
        this.classpathResourceScanner = classpathResourceScanner;
    }

    public String getAnnotatedClassesInitParam() {
        return annotatedClassesInitParam;
    }

    public void setAnnotatedClassesInitParam(String annotatedClassesInitParam) {
        this.annotatedClassesInitParam = annotatedClassesInitParam;
    }

    public String getAnnotatedClassesResourcePath() {
        return annotatedClassesResourcePath;
    }

    public void setAnnotatedClassesResourcePath(String annotatedClassesResourcePath) {
        this.annotatedClassesResourcePath = annotatedClassesResourcePath;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectConverter.class;
    }

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        if (componentManager.getServletContext() != null) {
            this.servletContext = componentManager.getServletContext();
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setContentTypesProvider(final ContentTypesProvider contentTypesProvider) {
        this.contentTypesProvider = contentTypesProvider;
    }

    @Override
    protected ObjectConverter createInstance() {
        List<Class<? extends HippoBean>> allAnnotatedClasses = null;
        List<Class<? extends HippoBean>> applicationAnnotatedClasses = null;

        if (annotatedClassesResourcePath == null && servletContext != null) {
            annotatedClassesResourcePath = servletContext.getInitParameter(annotatedClassesInitParam);
        }

        if (annotatedClassesResourcePath != null) {
            try {
                String[] resourcePaths = StringUtils.split(annotatedClassesResourcePath, ", \t\r\n");
                
                if (resourcePaths != null) {                
                    if (resourcePaths.length > GLOBAL_RESOURCE_PATH_SIZE) {
                        // set last GLOBAL_RESOURCE_PATH_SIZE paths as global paths
                        allAnnotatedClasses = ObjectConverterUtils.getAnnotatedClasses(classpathResourceScanner,
                                Arrays.copyOfRange(resourcePaths, resourcePaths.length - GLOBAL_RESOURCE_PATH_SIZE, resourcePaths.length));
                        // set other paths as application's paths 
                        applicationAnnotatedClasses = ObjectConverterUtils.getAnnotatedClasses(classpathResourceScanner,
                                Arrays.copyOfRange(resourcePaths, 0, resourcePaths.length - GLOBAL_RESOURCE_PATH_SIZE));
                        for (Class<? extends HippoBean> beanClass : applicationAnnotatedClasses) {
                            if (!allAnnotatedClasses.contains(beanClass)) {
                                allAnnotatedClasses.add(beanClass);
                            }
                        }
                    } else {
                        allAnnotatedClasses = ObjectConverterUtils.getAnnotatedClasses(classpathResourceScanner, resourcePaths);
                    }
                }
               
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        if (generateDynamicBean == null || generateDynamicBean.booleanValue()) {
            return new VersionedObjectConverterProxy(applicationAnnotatedClasses, allAnnotatedClasses, contentTypesProvider);
        } else {
            return ObjectConverterUtils.createObjectConverter(allAnnotatedClasses);
        }
    }

}

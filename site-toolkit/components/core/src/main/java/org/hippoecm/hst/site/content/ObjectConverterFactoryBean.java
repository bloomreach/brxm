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

import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
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

    private ServletContext servletContext;
    private ClasspathResourceScanner classpathResourceScanner;
    private String annotatedClassesInitParam = DefaultContentBeansTool.BEANS_ANNOTATED_CLASSES_CONF_PARAM;
    private String annotatedClassesResourcePath;

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

    @Override
    protected ObjectConverter createInstance() throws Exception {
        List<Class<? extends HippoBean>> annotatedClasses = null;

        if (annotatedClassesResourcePath == null && servletContext != null) {
            annotatedClassesResourcePath = servletContext.getInitParameter(annotatedClassesInitParam);
        }

        if (annotatedClassesResourcePath != null) {
            try {
                annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(classpathResourceScanner, StringUtils.split(annotatedClassesResourcePath, ", \t\r\n"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        if (annotatedClasses != null) {
            return ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }

        return null;
    }

}

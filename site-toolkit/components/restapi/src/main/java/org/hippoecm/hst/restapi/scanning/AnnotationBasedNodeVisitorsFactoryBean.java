/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.restapi.scanning;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.restapi.NodeVisitor;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.util.ClasspathResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.web.context.ServletContextAware;

public class AnnotationBasedNodeVisitorsFactoryBean extends AbstractFactoryBean<List<NodeVisitor>> implements ComponentManagerAware, ServletContextAware {

    private static final Logger log = LoggerFactory.getLogger(AnnotationBasedNodeVisitorsFactoryBean.class);

    private ServletContext servletContext;
    private ClasspathResourceScanner classpathResourceScanner;

    private static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";
    private String annotatedClassesInitParam = BEANS_ANNOTATED_CLASSES_CONF_PARAM;
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
        return List.class;
    }

    @Override // TODO #setServletContext is already invoked. It setComponentManager really needed? Perhaps for unit tests?
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
    protected List<NodeVisitor> createInstance() throws Exception {
        List<Class<? extends NodeVisitor>> annotatedClasses = null;

        if (annotatedClassesResourcePath == null && servletContext != null) {
            annotatedClassesResourcePath = servletContext.getInitParameter(annotatedClassesInitParam);
        }

        if (annotatedClassesResourcePath != null) {
            try {

                // add our annotated visitors package as well to the annotatedClassesResourcePath: org.hippoecm.hst.contentrestapi.annotated.visitors
                annotatedClassesResourcePath = annotatedClassesResourcePath + ",classpath*:org/hippoecm/hst/**/*.class";
                final String[] annotatedClassesResourcePaths = StringUtils.split(annotatedClassesResourcePath, ", \t\r\n");

                annotatedClasses = getAnnotatedClasses(classpathResourceScanner, annotatedClassesResourcePaths);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        if (annotatedClasses != null) {
            final List<NodeVisitor> nodeVisitors = createNodeVisitors(annotatedClasses);
            return nodeVisitors;
        }

        return Collections.emptyList();
    }


    public static List<Class<? extends NodeVisitor>> getAnnotatedClasses(final ClasspathResourceScanner resourceScanner, String ... locationPatterns) throws IOException {

        final List<Class<? extends NodeVisitor>> annotatedClasses = new ArrayList<>();
        final Set<String> annotatedClassNames = resourceScanner.scanClassNamesAnnotatedBy(PrimaryNodeTypeNodeVisitor.class, false, locationPatterns);

        if (annotatedClassNames != null && !annotatedClassNames.isEmpty()) {
            Class<?> clazz;

            for (String className : annotatedClassNames) {
                try {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    log.info("Skipped annotated class registration. The class cannot be loaded: {}.", className);
                    continue;
                }

                int mod = clazz.getModifiers();

                if (!Modifier.isPublic(mod)) {
                    log.info("Skipped annotated class registration. The class must be a *public* class: {}.", className);
                    continue;
                }

                if (NodeVisitor.class.isAssignableFrom(clazz)) {
                    annotatedClasses.add((Class<? extends NodeVisitor>) clazz);
                } else {
                    log.info("Skipped annotated class registration. The class must be type of {}: {}.", NodeVisitor.class, className);
                }
            }
        }

        return annotatedClasses;
    }

    public List<NodeVisitor> createNodeVisitors(final List<Class<? extends NodeVisitor>> annotatedClasses ) {
        // instantiate all annotatedClasses and constructor
        final List<NodeVisitor> nodeVisitors = new ArrayList<>();
        for (Class<? extends NodeVisitor> annotatedClass : annotatedClasses) {
            try {
                final NodeVisitor nodeVisitor = annotatedClass.newInstance();
                nodeVisitors.add(nodeVisitor);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return nodeVisitors;
    }

}

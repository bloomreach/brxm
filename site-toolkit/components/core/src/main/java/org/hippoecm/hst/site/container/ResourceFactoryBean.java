/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.container;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * ResourceFactoryBean
 * <P>
 * Simple Resource Factory Bean to create URL string or FILE from resource string for convenience,
 * depending on <code>objectType</code> constructor argument.
 * </P>
 * @version $Id$
 */
public class ResourceFactoryBean implements FactoryBean<Object>, ResourceLoaderAware {

    private static Logger log = LoggerFactory.getLogger(ResourceFactoryBean.class);

    private ResourceLoader resourceLoader;
    private String resourcePath;
    private Class<?> objectType;
    private boolean singleton = true;
    private Object singletonBean;
    private boolean ignoreCreationError;
    private Object defaultResourceObject;
    
    public ResourceFactoryBean(String resourcePath) {
        this(resourcePath, null);
    }
    
    public ResourceFactoryBean(String resourcePath, Class<?> objectType) {
        this(resourcePath, objectType, null);
    }
    
    public ResourceFactoryBean(String resourcePath, Class<?> objectType, Object defaultResourceObject) {
        this.resourcePath = resourcePath;
        this.objectType = objectType;
        this.defaultResourceObject = defaultResourceObject;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public Object getObject() throws Exception {
        if (singleton) {
            if (singletonBean == null) {
                singletonBean = createInstance();
                resourceLoader = null;
            }
            
            return singletonBean;
        } else {
            return createInstance();
        }
    }

    @Override
    public boolean isSingleton() {
        return singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    @Override
    public Class<?> getObjectType() {
        return (objectType != null ? objectType : Resource.class);
    }

    public boolean isIgnoreCreationError() {
        return ignoreCreationError;
    }

    public void setIgnoreCreationError(boolean ignoreCreationError) {
        this.ignoreCreationError = ignoreCreationError;
    }

    protected Object createInstance() throws Exception {
        Resource resource = null;

        try {
            resource = resourceLoader.getResource(resourcePath);

            if (URL.class == objectType) {
                return resource.getURL();
            } else if (String.class == objectType) {
                return resource.getURL().toString();
            } else if (URI.class == objectType) {
                return resource.getURI();
            } else if (File.class == objectType) {
                return resource.getFile();
            }
        } catch (Throwable th) {
            if (isIgnoreCreationError()) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to create resource, '{}'.", resourcePath, th);
                } else {
                    log.warn("Failed to create resource, '{}'. {}", resourcePath, th.toString());
                }

                // resource can be non-null if it throws an exception while resolving URL, URI or File.
                // whenever getting an exception, return the defaultResourceObject instead.
                if (defaultResourceObject != null) {
                    return defaultResourceObject;
                }
            } else {
                throw new BeanCreationException("Failed to create resource, '" + resourcePath + "'.", th);
            }
        }

        return resource;
    }

}

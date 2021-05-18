/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.manager;

import java.lang.ref.WeakReference;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.builder.HippoContentBean;
import org.hippoecm.hst.content.beans.dynamic.DynamicBeanDefinitionService;
import org.hippoecm.hst.content.beans.dynamic.DynamicBeanService;
import org.hippoecm.hst.content.beans.dynamic.DynamicBeanInterceptor;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

/**
 * Converts any document JCR node into a java bean. If necessary, create underlying class definition.
 */
public class DynamicObjectConverterImpl extends ObjectConverterImpl {

    private static final Logger log = LoggerFactory.getLogger(DynamicObjectConverterImpl.class);

    private final Map<String, Class<? extends DynamicBeanInterceptor>> dynamicBeanInterceptorPairs;
    private final DynamicBeanService dynamicBeanService;
    private final WeakReference<ContentTypes> contentTypesRef;

    DynamicObjectConverterImpl(final Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeBeanPairs,
            final Map<String, Class<? extends DynamicBeanInterceptor>> dynamicBeanInterceptorPairs,
            final String[] fallBackJcrNodeTypes, final ContentTypes contentTypes) {
        super(jcrPrimaryNodeTypeBeanPairs, fallBackJcrNodeTypes);

        this.dynamicBeanInterceptorPairs = dynamicBeanInterceptorPairs;

        // Store ContentTypes as a WeakReference so that
        // corresponding ObjectConverter cache entry at VersionedObjectConverterProxy could be eventually invalidated
        this.contentTypesRef = new WeakReference<>(contentTypes);

        dynamicBeanService = new DynamicBeanDefinitionService(this);

        jcrPrimaryNodeTypeBeanPairs.entrySet()
                .stream()
                .filter(entry -> shouldGenerateEnhancedBean(entry.getValue()))
                .forEach(entry -> dynamicBeanService.createBeanDefinition(new HippoContentBean(entry.getKey(), entry.getValue(), contentTypes.getType(entry.getKey()))));
    }

    public boolean shouldGenerateEnhancedBean(final Class<? extends HippoBean> existingBeanClass) {
        // if the HippoEssentialsGenerated is not defined on class or allowModifications filed is true, then the bean will be generated
        if (existingBeanClass != null) {
            final HippoEssentialsGenerated hippoEssentialsGenerated = existingBeanClass.getDeclaredAnnotation(HippoEssentialsGenerated.class);
            if (hippoEssentialsGenerated == null || hippoEssentialsGenerated.allowModifications()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getObject(final Node node) throws ObjectBeanManagerException {
        final String jcrPrimaryNodeType;
        final String path;
        try {

            final Node useNode = getActualNode(node);

            if (useNode.isSame(useNode.getSession().getRootNode()) && getAnnotatedClassFor("rep:root") == null) {
                log.debug("Root useNode is not mapped to be resolved to a bean.");
                return null;
            }

            if (useNode.isNodeType(NT_HANDLE)) {
                return useNode.hasNode(useNode.getName()) ? getObject(useNode.getNode(useNode.getName())) : null;
            }

            jcrPrimaryNodeType = useNode.getPrimaryNodeType().getName();
            if (jcrPrimaryNodeType.equals("hippotranslation:translations")) {
                log.info("Encountered node of type 'hippotranslation:translations' : This nodetype is completely deprecated and should be " +
                         "removed from all content including from prototypes.");
                return null;
            }


            Class<? extends HippoBean> delegateeClass = this.jcrPrimaryNodeTypeBeanPairs.get(jcrPrimaryNodeType);

            if (delegateeClass == null) {
                synchronized(this) {
                    //Check if other threads have already added the required type after the lock was released
                    delegateeClass = this.jcrPrimaryNodeTypeBeanPairs.get(jcrPrimaryNodeType);
                    if (delegateeClass == null) {
                        if (isDocumentType(useNode) || isCompoundType(useNode)) {
                            delegateeClass = createDynamicBeanDefinition(useNode);
                        }

                        if (delegateeClass == null) {
                            // no exact match, try a fallback type
                            delegateeClass = getFallbackClass(jcrPrimaryNodeType, useNode);
                        }
                    }
                }
            }

            if (delegateeClass != null) {
                return instantiateObject(delegateeClass, useNode);
            }
            path = useNode.getPath();
        } catch (RepositoryException e) {
            throw new ObjectBeanManagerException("Impossible to get the object from the repository", e);
        } catch (Exception e) {
            throw new ObjectBeanManagerException("Impossible to convert the useNode", e);
        }
        log.info("No Descriptor found for useNode '{}'. Cannot return a Bean for '{}'.", path, jcrPrimaryNodeType);
        return null;
    }

    @Override
    public void addBeanDefinition(@Nonnull final String documentType, @Nonnull final Class<? extends HippoBean> beanClass) {
        super.addBeanDefinition(documentType, beanClass);
    }

    private Class<? extends HippoBean> createDynamicBeanDefinition(final Node node) throws RepositoryException {
        final String documentType = node.getPrimaryNodeType().getName();

        final ContentTypes contentTypes = contentTypesRef.get();
        if (contentTypes == null) {
            //The object has been already garbage collected, in practice, it should never happen
            throw new IllegalStateException("The required ContentTypes object has been already garbage collected!");
        }

        final HippoContentBean contentBean = new HippoContentBean(documentType, null, contentTypes.getType(documentType));
        return dynamicBeanService.createBeanDefinition(contentBean);
    }
    
    /**
     * Return content type of the given document type name
     */
    @Nonnull
    public ContentType getContentType(final String name) {
        final ContentTypes contentTypes = contentTypesRef.get();
        if (contentTypes == null) {
            //The object has been already garbage collected, in practice, it should never happen
            throw new IllegalStateException("The required ContentTypes object has been already garbage collected!");
        }
        return contentTypes.getType(name);
    }

    public Class<? extends DynamicBeanInterceptor> getInterceptorDefinition(final String cmsType) {
        if (dynamicBeanInterceptorPairs == null) {
            return null;
        }
        return dynamicBeanInterceptorPairs.get(cmsType);
    }

}

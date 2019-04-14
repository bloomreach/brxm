/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;

import org.hippoecm.hst.content.beans.builder.AbstractBeanBuilderService;
import org.hippoecm.hst.content.beans.builder.HippoContentBean;
import org.hippoecm.hst.content.beans.manager.DynamicObjectConverterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

public class DynamicBeanDefinitionService extends AbstractBeanBuilderService implements DynamicBeanService {
    private static final Logger log = LoggerFactory.getLogger(DynamicBeanDefinitionService.class);

    private final DynamicObjectConverterImpl objectConverter;

    private class BeanInfo {
        private Class<? extends HippoBean> beanClass;
        private boolean updated;

        public Class<? extends HippoBean> getBeanClass() {
            return beanClass;
        }

        public boolean isUpdated() {
            return updated;
        }

        public BeanInfo(Class<? extends HippoBean> beanClass, boolean updated) {
            this.beanClass = beanClass;
            this.updated = updated;
        }
    }
    
    public DynamicBeanDefinitionService(final DynamicObjectConverterImpl objectConverter) {
        this.objectConverter = objectConverter;
    }

    private BeanInfo createDynamicCompoundBean(ContentType contentType) {
        if (contentType.getSuperTypes().stream().noneMatch(superType -> superType.equals(HippoNodeType.NT_COMPOUND))) {
            return null;
        }

        final BeanInfo parentBeanInfo = getOrCreateParentBean(contentType, true);
        return generateBean(parentBeanInfo, contentType);

    }

    @Override
    public Class<? extends HippoBean> createDynamicDocumentBeanDef(final Class<? extends HippoBean> parentBeanDef, final ContentType contentType) {
        BeanInfo generatedBeanDef = createDynamicDocumentBean(parentBeanDef != null ? new BeanInfo(parentBeanDef, false) : null, contentType);
        return generatedBeanDef.getBeanClass();
    }

    private BeanInfo createDynamicDocumentBean(BeanInfo parentBeanInfo, final ContentType contentType) {
        if (parentBeanInfo == null) {
            parentBeanInfo = getOrCreateParentBean(contentType, false);
        }
        return generateBean(parentBeanInfo, contentType);
    }
    
    
    private BeanInfo getOrCreateParentBean(final ContentType contentType, final boolean isCompound) {
        final String documentType = contentType.getName();
        final String projectNamespace = documentType.substring(0, documentType.indexOf(':'));

        final String parentDocumentType = contentType.getSuperTypes()
            .stream()
            .filter(superType -> superType.startsWith(projectNamespace))
            .findFirst()
            .orElse(null);

        if (parentDocumentType == null) {
            return getDefaultParentBeanInfo(isCompound);
        }

        final Class<? extends HippoBean> parentDocumentBeanDef = objectConverter.getClassFor(parentDocumentType);
        if (parentDocumentBeanDef == null) {
            final ContentType parentDocumentContentType = objectConverter.getContentType(parentDocumentType);
            if (parentDocumentContentType == null) {
                // parent bean is not in content type service, the default parent bean will return.
                return getDefaultParentBeanInfo(isCompound);
            }
            BeanInfo generatedBeanInfo = isCompound ? createDynamicCompoundBean(parentDocumentContentType)
                    : createDynamicDocumentBean(null, parentDocumentContentType);
            return generatedBeanInfo != null ? generatedBeanInfo : getDefaultParentBeanInfo(isCompound);
        } else {
            return new BeanInfo(parentDocumentBeanDef, false);
        }
    }

    private BeanInfo getDefaultParentBeanInfo(final boolean isCompound) {
        return new BeanInfo(isCompound ? HippoCompound.class : HippoDocument.class, true);
    }

    /**
     * Generates a bean by using byte buddy 
     * 
     * @param parentBean super class of the generated bean
     * @param contentType of the document type
     * @return Class definition
     */
    private BeanInfo generateBean(final BeanInfo parentBeanInfo, final ContentType contentType) {
        final HippoContentBean contentBean = new HippoContentBean("", contentType);

        final DynamicBeanBuilder builder = new DynamicBeanBuilder(
                DynamicBeanUtils.createJavaClassName(contentBean.getName()), parentBeanInfo.getBeanClass());

        generateMethodsByProperties(contentBean, builder);
        generateMethodsByNodes(contentBean, builder);
        
        if(!builder.isMethodAdded() && !parentBeanInfo.isUpdated()) {
            return parentBeanInfo;
        }

        final Class<? extends HippoBean> generatedBean = builder.create();
        objectConverter.addBeanDefinition(contentBean.getName(), generatedBean);
        log.info("Created dynamic bean {} from parent bean {}.", contentType.getName(), parentBeanInfo.getBeanClass().getSimpleName());

        return new BeanInfo(generatedBean, true);
    }

    @Override
    protected boolean hasChange(String name, boolean multiple, DynamicBeanBuilder builder) {
        return true;
    }

    @Override
    protected void addBeanMethodString(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {           
            builder.addBeanMethodString(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodCalendar(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {
            builder.addBeanMethodCalendar(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodBoolean(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {
            builder.addBeanMethodBoolean(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodLong(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {
            builder.addBeanMethodLong(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodDouble(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {
            builder.addBeanMethodDouble(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodDocbase(String name, boolean multiple, DynamicBeanBuilder builder) {
       final String methodName = DynamicBeanUtils.createMethodName(name);
       if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {
           builder.addBeanMethodDocbase(methodName, name, multiple);
        }
    }

    @Override
    protected void addCustomPropertyType(String name, boolean multiple, String type, DynamicBeanBuilder builder) {
        log.warn("Failed to create getter for property: {} of type: {}", name, type);
    }

    @Override
    protected void addBeanMethodHippoHtml(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {
            builder.addBeanMethodHippoHtml(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodImageLink(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {
            builder.addBeanMethodImageLink(methodName, name, multiple);
        }        
    }

    @Override
    protected void addBeanMethodHippoMirror(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {
            builder.addBeanMethodHippoMirror(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodHippoImage(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {
            builder.addBeanMethodHippoImage(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodHippoResource(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null) {
            builder.addBeanMethodHippoResource(methodName, name, multiple);
        }
    }

    @Override
    protected void addCustomNodeType(String name, boolean multiple, String type, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        
        Class<? extends HippoBean> generatedBeanDef = objectConverter.getClassFor(type);
        BeanInfo generatedBeanInfo = null;
        if (generatedBeanDef == null) {
            final ContentType compoundContentType = objectConverter.getContentType(type);
            if (compoundContentType == null) {
                return;
            }
            generatedBeanInfo = createDynamicCompoundBean(compoundContentType);
            if (generatedBeanInfo == null) {
                return;
            }
            generatedBeanDef = generatedBeanInfo.getBeanClass();
        }
        if (ClassUtils.getMethodIfAvailable(builder.getParentBean(), methodName) == null
                || (generatedBeanInfo != null && generatedBeanInfo.isUpdated())) {
            builder.addBeanMethodInternalType(generatedBeanDef, methodName, name, multiple);
        }
    }
}

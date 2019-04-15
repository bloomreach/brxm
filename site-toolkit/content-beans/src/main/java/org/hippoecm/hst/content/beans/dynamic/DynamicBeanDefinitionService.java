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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
        private final Class<? extends HippoBean> beanClass;
        private final boolean updated;

        Class<? extends HippoBean> getBeanClass() {
            return beanClass;
        }

        public boolean isUpdated() {
            return updated;
        }

        BeanInfo(final Class<? extends HippoBean> beanClass, final boolean updated) {
            this.beanClass = beanClass;
            this.updated = updated;
        }
    }
    
    public DynamicBeanDefinitionService(final DynamicObjectConverterImpl objectConverter) {
        this.objectConverter = objectConverter;
    }

    private BeanInfo createCompoundBeanDef(final ContentType contentType) {
        if (contentType.getSuperTypes().stream().noneMatch(superType -> superType.equals(HippoNodeType.NT_COMPOUND))) {
            return null;
        }

        final BeanInfo parentBeanInfo = getOrCreateParentBeanDef(contentType, true);
        return generateBeanDefinition(parentBeanInfo, contentType);
    }

    @Override
    public Class<? extends HippoBean> createDocumentBeanDef(final Class<? extends HippoBean> parentBeanDef, final ContentType contentType) {
        final BeanInfo generatedBeanDef = createDocumentBeanDef(contentType, parentBeanDef != null ? new BeanInfo(parentBeanDef, false) : null);
        return generatedBeanDef.getBeanClass();
    }

    private BeanInfo createDocumentBeanDef(@Nonnull final ContentType contentType, @Nullable BeanInfo parentBeanInfo) {
        if (parentBeanInfo == null) {
            parentBeanInfo = getOrCreateParentBeanDef(contentType, false);
        }
        return generateBeanDefinition(parentBeanInfo, contentType);
    }
    
    
    private BeanInfo getOrCreateParentBeanDef(@Nonnull final ContentType contentType, final boolean isCompound) {
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
            final BeanInfo generatedBeanInfo = isCompound ? createCompoundBeanDef(parentDocumentContentType)
                    : createDocumentBeanDef(parentDocumentContentType, null);
            return generatedBeanInfo != null ? generatedBeanInfo : getDefaultParentBeanInfo(isCompound);
        } else {
            return new BeanInfo(parentDocumentBeanDef, false);
        }
    }

    private BeanInfo getDefaultParentBeanInfo(final boolean isCompound) {
        return new BeanInfo(isCompound ? HippoCompound.class : HippoDocument.class, true);
    }

    /**
     * Generates a bean definition
     * 
     * @param parentBeanInfo information about super class of the generated bean definition
     * @param contentType of the document type
     * @return Class definition
     */
    private BeanInfo generateBeanDefinition(final BeanInfo parentBeanInfo, final ContentType contentType) {
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
    protected boolean hasChange(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
        return true;
    }

    @Override
    protected void addBeanMethodString(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
            builder.addBeanMethodString(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodCalendar(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
            builder.addBeanMethodCalendar(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodBoolean(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
            builder.addBeanMethodBoolean(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodLong(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
            builder.addBeanMethodLong(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodDouble(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
            builder.addBeanMethodDouble(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodDocbase(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
       final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
           builder.addBeanMethodDocbase(methodName, name, multiple);
        }
    }

    @Override
    protected void addCustomPropertyType(final String name, final boolean multiple, final String type, final DynamicBeanBuilder builder) {
        log.warn("Failed to create getter for property: {} of type: {}", name, type);
    }

    @Override
    protected void addBeanMethodHippoHtml(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
            builder.addBeanMethodHippoHtml(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodImageLink(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
            builder.addBeanMethodImageLink(methodName, name, multiple);
        }        
    }

    @Override
    protected void addBeanMethodHippoMirror(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
            builder.addBeanMethodHippoMirror(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodHippoImage(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
            builder.addBeanMethodHippoImage(methodName, name, multiple);
        }
    }

    @Override
    protected void addBeanMethodHippoResource(final String name, final boolean multiple, final DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        if (methodNotExists(builder.getParentBeanClass(), methodName)) {
            builder.addBeanMethodHippoResource(methodName, name, multiple);
        }
    }

    private boolean methodNotExists(@Nonnull final Class<?> clazz, @Nonnull final String methodName) {
        return ClassUtils.getMethodIfAvailable(clazz, methodName) == null;
    }

    @Override
    protected void addCustomNodeType(final String name, final boolean multiple, final String type, final DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        
        Class<? extends HippoBean> generatedBeanDef = objectConverter.getClassFor(type);
        BeanInfo generatedBeanInfo = null;
        if (generatedBeanDef == null) {
            final ContentType compoundContentType = objectConverter.getContentType(type);
            if (compoundContentType == null) {
                return;
            }
            generatedBeanInfo = createCompoundBeanDef(compoundContentType);
            if (generatedBeanInfo == null) {
                return;
            }
            generatedBeanDef = generatedBeanInfo.getBeanClass();
        }

        if (methodNotExists(builder.getParentBeanClass(), methodName)
                || (generatedBeanInfo != null && generatedBeanInfo.isUpdated())) {
            builder.addBeanMethodInternalType(generatedBeanDef, methodName, name, multiple);
        }
    }
}

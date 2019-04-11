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

import java.util.Set;

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

public class DynamicBeanDefinitionService extends AbstractBeanBuilderService implements DynamicBeanService {
    private static final Logger log = LoggerFactory.getLogger(DynamicBeanDefinitionService.class);

    private final DynamicObjectConverterImpl objectConverter;

    public DynamicBeanDefinitionService(final DynamicObjectConverterImpl objectConverter) {
        this.objectConverter = objectConverter;
    }

    private Class<? extends HippoBean> createDynamicCompoundBean(ContentType contentType) {
        final Set<String> superTypes = contentType.getSuperTypes();
        if (superTypes.isEmpty() || !superTypes.iterator().next().equals(HippoNodeType.NT_COMPOUND)) {
            return null;
        }

        final Class<? extends HippoBean> parentBeanDef = getOrCreateParentBean(contentType, true);
        return generateBean(parentBeanDef, contentType);
    }

    @Override
    public Class<? extends HippoBean> createDynamicDocumentBeanDef(Class<? extends HippoBean> parentBeanDef, final ContentType contentType) {
        if (parentBeanDef == null) {
            parentBeanDef = getOrCreateParentBean(contentType, false);
        }

        return generateBean(parentBeanDef, contentType);
    }

    private Class<? extends HippoBean> getOrCreateParentBean(final ContentType contentType, final boolean isCompound) {
        final String documentType = contentType.getName();
        final String projectNamespace = documentType.substring(0, documentType.indexOf(":"));

        final String parentDocumentType = contentType.getSuperTypes()
            .stream()
            .filter(superType -> superType.startsWith(projectNamespace))
            .findFirst()
            .orElse(null);

        if (parentDocumentType == null) {
            return getDefaultParentBean(isCompound);
        }

        Class<? extends HippoBean> generatedBeanDef = objectConverter.getClassFor(parentDocumentType);
        if (generatedBeanDef == null) {
            final ContentType parentDocumentContentType = objectConverter.getContentType(parentDocumentType);
            if (parentDocumentContentType == null) {
                return null;
            }

            if (isCompound) {
                generatedBeanDef = createDynamicCompoundBean(parentDocumentContentType);                
            } else {
                generatedBeanDef = createDynamicDocumentBeanDef(null, parentDocumentContentType);
            }

            if (generatedBeanDef == null) {
                generatedBeanDef = getDefaultParentBean(isCompound);
            }
        }

        return generatedBeanDef;
    }

    private Class<? extends HippoBean> getDefaultParentBean(final boolean isCompound) {
        return (isCompound) ? HippoCompound.class : HippoDocument.class;
    }

    /**
     * Generates a bean by using byte buddy 
     * 
     * @param parentBean super class of the generated bean
     * @param contentType of the document type
     * @return Class definition
     */
    private Class<? extends HippoBean> generateBean(final Class<? extends HippoBean> parentBean, final ContentType contentType) {
        final HippoContentBean contentBean = new HippoContentBean("", contentType);

        final DynamicBeanBuilder builder = new DynamicBeanBuilder(
                DynamicBeanUtils.createJavaClassName(contentBean.getName()), parentBean);

        generateMethodsByProperties(contentBean, builder);
        generateMethodsByNodes(contentBean, builder);

        final Class<? extends HippoBean> generatedBean = builder.create();
        objectConverter.addBeanDefinition(contentBean.getName(), generatedBean);
        log.info("Created dynamic bean {} from parent bean {}.", contentType.getName(), parentBean.getSimpleName());

        return generatedBean;
    }

    @Override
    protected boolean hasChange(String name, boolean multiple, DynamicBeanBuilder builder) {
        return true;
    }

    @Override
    protected void addBeanMethodString(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodString(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodCalendar(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodCalendar(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodBoolean(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodBoolean(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodLong(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodLong(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodDouble(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodDouble(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodDocbase(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodDocbase(methodName, name, multiple);
    }

    @Override
    protected void addCustomPropertyType(String name, boolean multiple, String type, DynamicBeanBuilder builder) {
        log.warn("Failed to create getter for property: {} of type: {}", name, type);
    }

    @Override
    protected void addBeanMethodHippoHtml(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodHippoHtml(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodImageLink(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodImageLink(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodHippoMirror(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodHippoMirror(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodHippoImage(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodHippoImage(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodHippoResource(String name, boolean multiple, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);
        builder.addBeanMethodHippoResource(methodName, name, multiple);
    }

    @Override
    protected void addCustomNodeType(String name, boolean multiple, String type, DynamicBeanBuilder builder) {
        final String methodName = DynamicBeanUtils.createMethodName(name);

        final ContentType compoundContentType = objectConverter.getContentType(type);
        if (compoundContentType == null) {
            return;
        }

        final Class<? extends HippoBean> generatedBeanDef = createDynamicCompoundBean(compoundContentType);
        if (generatedBeanDef == null) {
            return;
        }

        builder.addBeanMethodInternalType(generatedBeanDef, methodName, name, multiple);
    }
}

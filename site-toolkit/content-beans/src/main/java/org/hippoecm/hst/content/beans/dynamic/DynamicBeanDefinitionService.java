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
import org.hippoecm.hst.content.beans.builder.BeanBuilderServiceParameters;
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

    class DynamicBeanBuilderServiceParameters implements BeanBuilderServiceParameters {
        private DynamicBeanBuilder builder;

        DynamicBeanBuilderServiceParameters(final DynamicBeanBuilder builder) {
            this.builder = builder;
        }

        DynamicBeanBuilder getDynamicBeanBuilder() {
            return builder;
        }
    }

    public DynamicBeanDefinitionService(final DynamicObjectConverterImpl objectConverter) {
        this.objectConverter = objectConverter;
    }

    private Class<? extends HippoBean> createDynamicCompoundBean(ContentType contentType) {
        final Set<String> superTypes = contentType.getSuperTypes();
        if (superTypes.size() == 0 || !superTypes.iterator().next().equals(HippoNodeType.NT_COMPOUND)) {
            return null;
        }

        return generateBean(HippoCompound.class, contentType);
    }

    @Override
    public Class<? extends HippoBean> createDynamicDocumentBeanDef(Class<? extends HippoBean> parentBeanDef, final ContentType contentType) {

        if (parentBeanDef == null) {
            parentBeanDef = HippoDocument.class;
        }

        return generateBean(parentBeanDef, contentType);
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
        final BeanBuilderServiceParameters builderParameters = new DynamicBeanBuilderServiceParameters(builder);

        generateMethodsByProperties(contentBean, builderParameters);
        generateMethodsByNodes(contentBean, builderParameters, contentType);

        final Class<? extends HippoBean> generatedBean = builder.create();
        objectConverter.addBeanDefinition(contentBean.getName(), generatedBean);

        return generatedBean;
    }

    @Override
    protected boolean hasChange(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        return true;
    }

    @Override
    protected void addBeanMethodString(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodString(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodCalendar(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodCalendar(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodBoolean(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodBoolean(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodLong(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodLong(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodDouble(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodDouble(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodDocbase(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodDocbase(methodName, name, multiple);
    }

    @Override
    protected void addCustomPropertyType(String name, boolean multiple, String type, BeanBuilderServiceParameters builderParameters) {
        log.warn("Failed to create getter for property: {} of type: {}", name, type);
    }

    @Override
    protected void addBeanMethodHippoHtml(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodHippoHtml(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodImageLink(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodImageLink(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodHippoMirror(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodHippoMirror(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodHippoImage(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodHippoImage(methodName, name, multiple);
    }

    @Override
    protected void addBeanMethodHippoResource(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodHippoResource(methodName, name, multiple);
    }

    @Override
    protected void addCustomNodeType(String name, boolean multiple, ContentType contentType, BeanBuilderServiceParameters builderParameters) {

        DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);

        Class<? extends HippoBean> generatedBeanDef = objectConverter.getClassFor(name);
        if (generatedBeanDef == null) {
            generatedBeanDef = createDynamicCompoundBean(contentType);
        }

        parameters.getDynamicBeanBuilder().addBeanMethodInternalType(generatedBeanDef, methodName, name, multiple);
    }
}

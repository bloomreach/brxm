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

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.builder.AbstractBeanBuilderService;
import org.hippoecm.hst.content.beans.builder.BeanBuilderServiceParameters;
import org.hippoecm.hst.content.beans.builder.HippoContentBean;
import org.hippoecm.hst.content.beans.dynamic.DynamicBeanService;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoDynamicBeanService extends AbstractBeanBuilderService implements DynamicBeanService {
    private static final Logger log = LoggerFactory.getLogger(HippoDynamicBeanService.class);

    private ObjectConverter objectConverter;

    class DynamicBeanBuilderServiceParameters implements BeanBuilderServiceParameters {
        private DynamicBeanBuilder builder;

        public DynamicBeanBuilderServiceParameters(final DynamicBeanBuilder builder) {
            this.builder = builder;
        }

        public DynamicBeanBuilder getDynamicBeanBuilder() {
            return builder;
        }
    }

    public HippoDynamicBeanService(final ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    private Class<? extends HippoBean> createDynamicCompoundBean(ContentType contentType) {
        final Set<String> superTypes = contentType.getSuperTypes();
        if (superTypes.size() == 0 || !superTypes.iterator().next().equals(HippoNodeType.NT_COMPOUND)) {
            return null;
        }

        Class<? extends HippoBean> parentBean = HippoCompound.class;

        return generateBean(parentBean, contentType);
    }

    @Override
    public Class<? extends HippoBean> createDynamicDocumentBean(Class<? extends HippoBean> parentBean,
            final String documentType) {
        ContentTypeService contentTypeService = getContentTypeService();
        if (contentTypeService == null) {
            return null;
        }

        final ContentType contentType;
        try {
            contentType = contentTypeService.getContentTypes().getType(documentType);
        } catch (RepositoryException e) {
            log.error("Exception occured while getting the content type from ContentTypeService.");
            return null;
        }

        if (parentBean == null) {
            parentBean = HippoDocument.class;
        }

        return generateBean(parentBean, contentType);
    }

    /**
     * Generates a bean by using byte buddy 
     * 
     * @param parentBean super class of the generated bean
     * @param contentType of the document type
     * @return
     */
    private Class<? extends HippoBean> generateBean(final Class<? extends HippoBean> parentBean, final ContentType contentType) {
        final HippoContentBean contentBean = new HippoContentBean("", contentType);

        final DynamicBeanBuilder builder = new DynamicBeanBuilder(
                DynamicBeanUtils.createJavaClassName(contentBean.getName()), parentBean);
        final BeanBuilderServiceParameters builderParameters = new DynamicBeanBuilderServiceParameters(builder);

        super.generateMethodsByProperties(contentBean, builderParameters);
        super.generateMethodsByNodes(contentBean, builderParameters);

        final Class<? extends HippoBean> generatedBean = builder.create();
        ObjectConverterUtils.updateDynamicBeanDefinition(generatedBean, contentBean.getName(), objectConverter);

        return generatedBean;
    }

    @Override
    public boolean hasChange(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        return true;
    }

    @Override
    public void addBeanMethodString(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodString(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodCalendar(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodCalendar(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodBoolean(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodBoolean(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodLong(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodLong(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodDouble(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodDouble(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodDocbase(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodDocbase(methodName, name, multiple);
    }

    @Override
    public void addCustomPropertyType(String name, boolean multiple, String type, BeanBuilderServiceParameters builderParameters) {
        log.warn("Failed to create getter for property: {} of type: {}", name, type);
    }

    @Override
    public void addBeanMethodHippoHtml(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodHippoHtml(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodImageLink(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodImageLink(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodHippoMirror(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodHippoMirror(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodHippoImage(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodHippoImage(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodHippoResource(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getDynamicBeanBuilder().addBeanMethodHippoResource(methodName, name, multiple);
    }

    @Override
    public void addCustomNodeType(String name, boolean multiple, String type, BeanBuilderServiceParameters builderParameters) {
        ContentTypeService contentTypeService = getContentTypeService();
        if (contentTypeService == null) {
            return;
        }

        DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);

        Class<? extends HippoBean> generatedBean = objectConverter.getAnnotatedClassFor(name);
        if (generatedBean == null) {
            final ContentType contentType;
            try {
                contentType = contentTypeService.getContentTypes().getType(name);
            } catch (RepositoryException e) {
                log.error("Exception occured while getting the content type from ContentTypeService.");
                return;
            }
            generatedBean = createDynamicCompoundBean(contentType);
        }

        parameters.getDynamicBeanBuilder().addBeanMethodInternalType(generatedBean, methodName, name, multiple);
    }

    private ContentTypeService getContentTypeService() {
        ContentTypeService contentTypeService = HippoServiceRegistry.getService(ContentTypeService.class);
        if (contentTypeService == null) {
            log.warn("ContentTypeService hasn't been initialized yet.");
        }

        return contentTypeService;
    }

}

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
package org.hippoecm.hst.content.beans.dynamic;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_GALLERYTYPE;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.builder.AbstractBeanBuilderService;
import org.hippoecm.hst.content.beans.builder.HippoContentBean;
import org.hippoecm.hst.content.beans.manager.DynamicObjectConverterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

public class DynamicBeanDefinitionService extends AbstractBeanBuilderService implements DynamicBeanService {
    private static final Logger log = LoggerFactory.getLogger(DynamicBeanDefinitionService.class);
    private static final String GALLERY_IMAGESET_NODETYPE = "hippogallery:imageset";
    
    private final DynamicObjectConverterImpl objectConverter;

    private String[] galleryTypes;

    public DynamicBeanDefinitionService(final DynamicObjectConverterImpl objectConverter) {
        this.objectConverter = objectConverter;
        assignGalleryTypes();
    }

    private void assignGalleryTypes() {
        final ComponentManager componentManager = HstServices.getComponentManager();
        if (componentManager == null) {
            galleryTypes = new String[] { GALLERY_IMAGESET_NODETYPE };
            return;
        }

        Session session = null;
        try {
            session = getSession(componentManager);
            try {
                final HippoBean gallery = (HippoBean) objectConverter.getObject(session, "/content/gallery");
                if (gallery == null) {
                    log.warn("The 'content/gallery' node is not found, the default type 'HippoGalleryImageSet' will be used for dynamic beans.");
                    galleryTypes = new String[] { GALLERY_IMAGESET_NODETYPE };
                } else {
                    galleryTypes = gallery.getMultipleProperty(HIPPOSTD_GALLERYTYPE);
                }
            } catch (ObjectBeanManagerException e) {
                log.warn("Failed to get the gallery type(s), the default type 'HippoGalleryImageSet' will be used for dynamic beans.");
                galleryTypes = new String[] { GALLERY_IMAGESET_NODETYPE };
            }
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        } finally {
            closeSession(session);
        }
    }

    private Session getSession(final ComponentManager componentManager) throws RepositoryException {
        final Repository repository = componentManager.getComponent(Repository.class.getName());
        final Credentials configUser = componentManager.getComponent(Credentials.class.getName() + ".hstconfigreader");
        return repository.login(configUser);
    }

    private void closeSession(final Session session) {
        if (session != null) {
            session.logout();
        }
    }

    private Node getDocumentTypeNode(final String documentType) {
        final ComponentManager componentManager = HstServices.getComponentManager();
        if (componentManager == null) {
            log.error("HST Services haven't been initialized yet.");
            return null;
        }

        Session session = null;
        try {
            session = getSession(componentManager);

            final String documentTypeNodePath = HippoNodeType.NAMESPACES_PATH + "/"
                    + StringUtils.substringBefore(documentType, ":") + "/" + StringUtils.substringAfter(documentType, ":");

            return session.getRootNode().getNode(documentTypeNodePath);
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        } finally {
            closeSession(session);
        }
    }

    @Override
    public Class<? extends HippoBean> createBeanDefinition(final HippoContentBean contentBean) {
        if (!contentBean.hasContentType()) {
            log.error("ContentType of the document type {} doesn't exist in the ContentTypeService.", contentBean.getDocumentType());
            return null;
        }

        if (contentBean.getParentBean() == null && contentBean.getParentDocumentType() == null) {
            log.error("Document {} can't be generated because it doesn't have any relevant supertypes.", contentBean.getDocumentType());
            return null;
        }

        if (contentBean.getParentBean() == null) {
            setOrCreateParentBeanDefinition(contentBean);
        }

        return generateBeanDefinition(contentBean);
    }

    /**
     * Searchs a parent bean definition in objectConverter. If there is a matching parent
     * bean definition, uses the definition. Otherwise, a pre-defined base bean will be used
     * as a parent bean.
     * 
     * @param contentBean content of the runtime bean to be generated
     * @return
     */
    private void setOrCreateParentBeanDefinition(final HippoContentBean contentBean) {
        final Class<? extends HippoBean> parentBean = objectConverter.getClassFor(contentBean.getParentDocumentType());
        if (parentBean == null) {
            final ContentType parentDocumentContentType = objectConverter.getContentType(contentBean.getParentDocumentType());
            final HippoContentBean parentRuntimeBeanContent = new HippoContentBean(contentBean.getParentDocumentType(), parentDocumentContentType);

            contentBean.forceGeneration();
            contentBean.setParentBean(createBeanDefinition(parentRuntimeBeanContent));
        } else {
            contentBean.setParentBean(parentBean);
        }
    }

    /**
     * Generates a bean definition
     * 
     * @param contentBean content of the runtime bean to be generated
     * @return Class definition
     */
    private Class<? extends HippoBean> generateBeanDefinition(final HippoContentBean contentBean) {
        log.info("Creating bean of document type {} from parent bean {}.", contentBean.getDocumentType(), contentBean.getParentBean().getSimpleName());

        final DynamicBeanBuilder builder = new DynamicBeanBuilder(
                DynamicBeanUtils.createJavaClassName(contentBean.getDocumentType()), contentBean.getParentBean());

        generateMethodsByProperties(contentBean, builder);
        generateMethodsByChildNodes(contentBean, builder);

        if(!builder.isMethodAdded() && !contentBean.isParentReloaded()) {
            log.info("Bean {} is not enhanced because it doesn't have any missing methods.", contentBean.getParentBean().getSimpleName());
            return contentBean.getParentBean();
        }

        final Class<? extends HippoBean> generatedBean = builder.create();
        objectConverter.addBeanDefinition(contentBean.getDocumentType(), generatedBean);
        log.info("Bean {} is created.", generatedBean.getSimpleName());

        return generatedBean;
    }

    @Override
    protected boolean hasChange(final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        // creates the method if it doesn't exist on the parent bean 
        return ClassUtils.getMethodIfAvailable(builder.getParentBeanClass(), methodName) == null;
    }

    @Override
    protected void addBeanMethodString(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodString(methodName, propertyName, multiple);
    }

    @Override
    protected void addBeanMethodCalendar(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodCalendar(methodName, propertyName, multiple);
    }

    @Override
    protected void addBeanMethodBoolean(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodBoolean(methodName, propertyName, multiple);
    }

    @Override
    protected void addBeanMethodLong(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodLong(methodName, propertyName, multiple);
    }

    @Override
    protected void addBeanMethodDouble(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodDouble(methodName, propertyName, multiple);
    }

    @Override
    protected void addBeanMethodDocbase(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
       builder.addBeanMethodDocbase(methodName, propertyName, multiple);
    }

    @Override
    protected void addCustomPropertyType(final String propertyName, final String methodName, final boolean multiple, final String documentType, final String type, final DynamicBeanBuilder builder) {
        final Class<? extends HippoBean> generatedBeanDefinition = getOrCreateCustomBean(type);
        if (generatedBeanDefinition == null) {
            log.warn("Failed to create getter for property: {} of type: {}", propertyName, type);
            return;
        }

        final Node documentTypeNode = getDocumentTypeNode(documentType);
        if (documentTypeNode == null) {
            return;
        }

        builder.addBeanMethodCustomField(generatedBeanDefinition, methodName, propertyName, multiple, documentTypeNode);
    }

    @Override
    protected void addBeanMethodHippoHtml(final String propertyName, final String methodName, boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodHippoHtml(methodName, propertyName, multiple);
    }

    private Class<? extends HippoBean> getGalleryImageSetTypeClass() {
        if (galleryTypes == null || galleryTypes[0].equals(GALLERY_IMAGESET_NODETYPE) || galleryTypes.length > 1) {
            if (galleryTypes != null && galleryTypes.length > 1) {
                log.warn("More than one gallery type is defined, the default type 'HippoGalleryImageSet' will be used for dynamic beans.");
            }
            return HippoGalleryImageSet.class;
        } else {
            Class<? extends HippoBean> galleryBeanDefinition = objectConverter.getClassFor(galleryTypes[0]);
            if (galleryBeanDefinition == null) {
                final ContentType galleryContentType = objectConverter.getContentType(galleryTypes[0]);
                if (galleryContentType == null) {
                    return HippoGalleryImageSet.class;
                }

                final HippoContentBean galleryBeanContent = new HippoContentBean(galleryTypes[0], HippoGalleryImageSet.class, galleryContentType);
                galleryBeanContent.forceGeneration();
                galleryBeanDefinition = generateBeanDefinition(galleryBeanContent);
            }
            return galleryBeanDefinition;
        }
    }

    @Override
    protected void addBeanMethodImageLink(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodImageLink(methodName, propertyName, getGalleryImageSetTypeClass(), multiple);
    }

    @Override
    protected void addBeanMethodHippoMirror(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodHippoMirror(methodName, propertyName, multiple);
    }

    @Override
    protected void addBeanMethodHippoImage(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodHippoImage(methodName, propertyName, multiple);
    }

    @Override
    protected void addBeanMethodHippoResource(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodHippoResource(methodName, propertyName, multiple);
    }

    @Override
    protected void addBeanMethodContentBlocks(final String propertyName, final String methodName, final boolean multiple, final DynamicBeanBuilder builder) {
        builder.addBeanMethodContentBlocks(methodName, propertyName, multiple);
    }

    @Override
    protected void addBeanMethodCompoundType(final String propertyName, final String methodName, final boolean multiple, final String type, final DynamicBeanBuilder builder) {
        final Class<? extends HippoBean> generatedBeanDefinition = getOrCreateCustomBean(type);
        if (generatedBeanDefinition == null) {
            return;
        }

        builder.addBeanMethodCompoundType(methodName, propertyName, multiple);
    }

    @Override
    protected void addCustomNodeType(final String propertyName, final String methodName, final boolean multiple, final String type, final DynamicBeanBuilder builder) {
        final Class<? extends HippoBean> generatedBeanDefinition = getOrCreateCustomBean(type);
        if (generatedBeanDefinition == null) {
            return;
        }

        builder.addBeanMethodInternalType(methodName, generatedBeanDefinition, propertyName, multiple);
    }

    private Class<? extends HippoBean> getOrCreateCustomBean(final String documentType) {
        Class<? extends HippoBean> generatedBeanDefinition = objectConverter.getClassFor(documentType);
        if (generatedBeanDefinition == null) {
            // Custom generic bean should be created because the document type doesn't exist in objectConverter
            final ContentType contentType = objectConverter.getContentType(documentType);
            if (contentType == null) {
                return null;
            }

            final HippoContentBean contentBean = new HippoContentBean(documentType, contentType);
            // regardless of a change on this document type was happened or not, reload that
            contentBean.forceGeneration();
            generatedBeanDefinition = createBeanDefinition(contentBean);
        }
        return generatedBeanDefinition;
    }

}

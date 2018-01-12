/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.onehippo.cms7.essentials.plugin.sdk.model.UserFeedback;
import org.onehippo.cms7.essentials.plugin.sdk.service.JcrService;
import org.onehippo.cms7.essentials.plugin.sdk.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.service.SettingsService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.beansmodel.HippoContentBean;
import org.onehippo.cms7.essentials.plugin.sdk.utils.beansmodel.HippoContentChildNode;
import org.onehippo.cms7.essentials.plugin.sdk.utils.beansmodel.HippoContentProperty;
import org.onehippo.cms7.essentials.plugin.sdk.utils.beansmodel.HippoEssentialsGeneratedObject;
import org.onehippo.cms7.essentials.plugin.sdk.utils.code.EssentialsGeneratedMethod;
import org.onehippo.cms7.essentials.plugin.sdk.utils.code.ExistingMethodsVisitor;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @version "$Id$"
 */
@Service
public class ContentBeansServiceImpl implements ContentBeansService {

    private static final Logger log = LoggerFactory.getLogger(ContentBeansServiceImpl.class);

    private static final String HIPPO_GALLERY_IMAGE_SET_BEAN = "HippoGalleryImageSetBean";
    private static final String HIPPO_GALLERY_IMAGE_SET_CLASS = "HippoGalleryImageSet";
    private static final String RELATED_MIXIN = "relateddocs:relatabledocs";
    private static final String DOCBASE = "Docbase";
    private static final String RESOURCE = "hippo:resource";
    private static final String BASE_COMPOUND_TYPE = "hippo:compound";

    @Inject private ProjectService projectService;
    @Inject private SettingsService settingsService;

    /**
     * How many loops we run (beans extending none existing beans)
     */
    private static final int MISSING_DEPTH_MAX = 5;
    private int missingBeansDepth = 0;

    @Override
    public void createBeans(final JcrService jcrService, final UserFeedback feedback, final String imageSetClassName) {
        final Set<HippoContentBean> contentBeans = getContentBeans(jcrService);
        final Map<String, Path> existing = findBeans();
        final List<HippoContentBean> missingBeans = Lists.newArrayList(filterMissingBeans(contentBeans, existing));
        final Iterator<HippoContentBean> missingBeanIterator = missingBeans.iterator();
        for (; missingBeanIterator.hasNext(); ) {
            final HippoContentBean missingBean = missingBeanIterator.next();
            // check if directly extending compound:
            final Set<String> superTypes = missingBean.getSuperTypes();
            if (superTypes.size() == 1 && superTypes.iterator().next().equals(BASE_COMPOUND_TYPE)) {
                createCompoundBaseBean(missingBean, feedback);
                missingBeanIterator.remove();
            } else {
                final String parent = findExistingParent(missingBean, existing);
                if (parent != null) {
                    log.debug("found parent: {}, {}", parent, missingBean);
                    missingBeanIterator.remove();
                    final Path parentPath = existing.get(parent);
                    if (parentPath == null) {
                        log.error("Couldn't find parent bean for: {}", parent);
                        if (parent.equals(getBaseSupertype())) {
                            log.error("Base document type is missing: {}", parent);
                        }
                        continue;
                    }
                    createBean(missingBean, parentPath, feedback);
                }
            }
        }
        // process beans without resolved parent beans
        processMissing(missingBeans, feedback, imageSetClassName);
        processProperties(contentBeans, feedback, imageSetClassName);
        // check if still missing(beans that extended missing beans)
        final Iterator<HippoContentBean> extendedMissing = filterMissingBeans(contentBeans, findBeans());
        final boolean hasNonCreatedBeans = extendedMissing.hasNext();
        if (missingBeansDepth < MISSING_DEPTH_MAX && hasNonCreatedBeans) {
            missingBeansDepth++;
            createBeans(jcrService, feedback, imageSetClassName);
        } else if (hasNonCreatedBeans) {
            log.error("Not all beans were created: {}", extendedMissing);
        }
        processRelatedDocuments(contentBeans, feedback);
    }


    /**
     * Removes methods which are annotated but missing within content services
     */
    @Override
    public void cleanupMethods(final JcrService jcrService, final UserFeedback feedback) {
        final Set<HippoContentBean> beans = getContentBeans(jcrService);
        final Map<String, Path> existing = findBeans();
        for (HippoContentBean bean : beans) {
            final Path path = existing.get(bean.getName());
            if (path != null) {
                final Set<String> properties = extractInternalNames(bean);
                final ExistingMethodsVisitor methodCollection = JavaSourceUtils.getMethodCollection(path);

                final List<EssentialsGeneratedMethod> generatedMethods = methodCollection.getGeneratedMethods();
                for (EssentialsGeneratedMethod method : generatedMethods) {
                    final String internalName = method.getInternalName();
                    if (!properties.contains(internalName)) {
                        if (internalName.equals(EssentialConst.RELATEDDOCS_DOCS)) {
                            // check if we have mixin:
                            if (bean.getContentType().getAggregatedTypes().contains(RELATED_MIXIN)) {
                                log.info("Skipping deletion of {}, mixin type", internalName);
                                continue;
                            }
                        }
                        final HippoEssentialsGeneratedObject annotation = JavaSourceUtils.getHippoEssentialsAnnotation(path, method.getMethodDeclaration());
                        final boolean allowMethodUpdate = annotation != null && annotation.isAllowModifications();
                        final HippoEssentialsGeneratedObject classAnnotation = JavaSourceUtils.getHippoGeneratedAnnotation(path);
                        final boolean allowClassUpdate = classAnnotation != null && classAnnotation.isAllowModifications();
                        final String methodName = method.getMethodName();
                        if (!allowClassUpdate) {
                            logClassModificationDisabled(path, methodName, feedback);
                        } else if (allowMethodUpdate) {
                            log.info("@Missing declaration for: {}. Method will be deleted", internalName);
                            final boolean deleted = JavaSourceUtils.deleteMethod(method, path);
                            if (deleted) {
                                logMethodDeleted(path, methodName, feedback);
                            } else {
                                final String message = String.format("Failed to delete method '%s' from bean '%s'.", methodName, path);
                                feedback.addError(message);
                            }
                        } else {
                            logMethodModificationDisabled(path, methodName, feedback);
                        }
                    }
                }
            }
        }
    }


    private Set<String> extractInternalNames(final HippoContentBean bean) {
        final Set<String> set = new HashSet<>();
        final List<HippoContentProperty> properties = bean.getProperties();
        if (properties != null) {
            for (HippoContentProperty property : properties) {
                set.add(property.getName());
            }
        }
        final List<HippoContentChildNode> children = bean.getChildren();
        if (children != null) {
            for (HippoContentChildNode child : children) {
                set.add(child.getName());
            }
        }

        return set;
    }


    private void processRelatedDocuments(final Set<HippoContentBean> contentBeans, final UserFeedback feedback) {
        boolean hasRelatedDocs = false;
        for (HippoContentBean contentBean : contentBeans) {
            if (contentBean.getContentType().getAggregatedTypes().contains(RELATED_MIXIN)) {
                hasRelatedDocs = true;
            }
        }
        if (!hasRelatedDocs) {
            return;
        }

        Map<String, Path> existing = findBeans();
        for (HippoContentBean contentBean : contentBeans) {
            final Path path = existing.get(contentBean.getName());
            if (path != null && contentBean.getContentType().getAggregatedTypes().contains(RELATED_MIXIN)) {
                final ExistingMethodsVisitor methodCollection = JavaSourceUtils.getMethodCollection(path);
                final Set<String> methodInternalNames = methodCollection.getMethodInternalNames();
                if (!methodInternalNames.contains(EssentialConst.RELATEDDOCS_DOCS)) {
                    JavaSourceUtils.addRelatedDocsMethod(EssentialConst.METHOD_RELATED_DOCUMENTS, path);
                    logMethodCreated(path, EssentialConst.METHOD_RELATED_DOCUMENTS, feedback);
                }
            }
        }
    }


    /*
      public List<HippoBean> getRelatedDocs() {

        RelatedDocsBean bean = this.getBean("relateddocs:docs");
        return bean.getDocs();

    }
     */
    public void addRelatedDocsMethod(final Path bean) {

    }


    private Iterator<HippoContentBean> filterMissingBeans(final Set<HippoContentBean> contentBeans, final Map<String, Path> existing) {
        final Iterable<HippoContentBean> missingBeans = Iterables.filter(contentBeans, new Predicate<HippoContentBean>() {
            @Override
            public boolean apply(HippoContentBean b) {
                return !existing.containsKey(b.getName());
            }
        });
        // process beans with known (project) supertypes:
        return Lists.newArrayList(missingBeans).iterator();
    }


    private void processProperties(final Set<HippoContentBean> contentBeans, final UserFeedback feedback, final String imageSetClassName) {
        final Map<String, Path> existing = findBeans();
        for (HippoContentBean bean : contentBeans) {
            final Path beanPath = existing.get(bean.getName());
            if (beanPath != null) {
                final String parent = findExistingParent(bean, existing);
                final Path parentPath = existing.get(parent);
                if (parentPath != null) {
                    final ExistingMethodsVisitor parentMethodCollection = JavaSourceUtils.getMethodCollection(parentPath);
                    final ExistingMethodsVisitor ownMethodCollection = JavaSourceUtils.getMethodCollection(beanPath);
                    final Set<String> existingMethods = ownMethodCollection.getMethodInternalNames();
                    existingMethods.addAll(parentMethodCollection.getMethodInternalNames());
                    addMethods(bean, beanPath, existingMethods, feedback, imageSetClassName);
                } else {
                    final ExistingMethodsVisitor ownMethodCollection = JavaSourceUtils.getMethodCollection(beanPath);
                    addMethods(bean, beanPath, ownMethodCollection.getMethodInternalNames(), feedback, imageSetClassName);
                }

            }
        }
    }


    private void processMissing(final List<HippoContentBean> missingBeans, final UserFeedback feedback, final String imageSetClassName) {
        for (HippoContentBean missingBean : missingBeans) {
            final SortedSet<String> mySupertypes = missingBean.getContentType().getSuperTypes();
            if (mySupertypes.contains("hippogallery:relaxed")) {
                final Path javaClass = createJavaClass(missingBean, feedback);
                JavaSourceUtils.createHippoBean(javaClass, settingsService.getSettings().getSelectedBeansPackage(), missingBean.getName(), missingBean.getName());
                JavaSourceUtils.addExtendsClass(javaClass, HIPPO_GALLERY_IMAGE_SET_CLASS);
                JavaSourceUtils.addImport(javaClass, EssentialConst.HIPPO_IMAGE_SET_IMPORT);
                addMethods(missingBean, javaClass, new ArrayList<>(), feedback, imageSetClassName);
            }
        }
    }

    private String findExistingParent(final HippoContentBean missingBean, final Map<String, Path> existing) {
        final Set<String> superTypes = missingBean.getSuperTypes();
        final String baseSupertype = getBaseSupertype();
        if (superTypes.size() == 1 && superTypes.iterator().next().equals(baseSupertype)) {
            return baseSupertype;
        }
        // extends a document
        for (String superType : superTypes) {
            if (!superType.equals(baseSupertype) && existing.containsKey(superType)) {
                // TODO improve nested types
                return superType;
            }
        }
        return null;
    }


    private Set<HippoContentBean> getContentBeans(final JcrService jcrService) {
        try {
            final String namespace = settingsService.getSettings().getProjectNamespace();
            final Set<HippoContentBean> beans = new HashSet<>();
            final Set<ContentType> projectContentTypes = getProjectContentTypes(jcrService);
            for (ContentType projectContentType : projectContentTypes) {
                final HippoContentBean bean = new HippoContentBean(namespace, projectContentType);
                beans.add(bean);
            }
            return beans;
        } catch (RepositoryException e) {
            log.error("Error fetching beans", e);
        }
        return Collections.emptySet();
    }


    /**
     * Fetch project content types
     *
     * @return empty collection if no types are found
     * @throws RepositoryException for unexpected Repository situations
     */
    private Set<ContentType> getProjectContentTypes(final JcrService jcrService) throws RepositoryException {
        final String namespacePrefix = settingsService.getSettings().getProjectNamespace();
        final Set<ContentType> projectContentTypes = new HashSet<>();
        final Session session = jcrService.createSession();
        try {
            final ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
            final ContentTypes contentTypes = service.getContentTypes();
            final SortedMap<String, Set<ContentType>> typesByPrefix = contentTypes.getTypesByPrefix();
            for (Map.Entry<String, Set<ContentType>> entry : typesByPrefix.entrySet()) {
                final String key = entry.getKey();
                final Set<ContentType> value = entry.getValue();
                if (key.equals(namespacePrefix)) {
                    projectContentTypes.addAll(value);
                    return projectContentTypes;
                }
            }
        } finally {
            jcrService.destroySession(session);
        }
        return Collections.emptySet();
    }

    @Override
    public Map<String, Path> findBeans() {
        final Path startDir = projectService.getBeansPackagePath();
        final Map<String, Path> existingBeans = new HashMap<>();
        final List<Path> directories = new ArrayList<>();
        GlobalUtils.populateDirectories(startDir, directories);
        final String pattern = "*.java";
        for (Path directory : directories) {
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pattern)) {
                for (Path path : stream) {
                    final String nodeJcrType = JavaSourceUtils.getNodeJcrType(path);
                    if (nodeJcrType != null) {
                        existingBeans.put(nodeJcrType, path);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading java files", e);
            }
        }
        return existingBeans;

    }


    private void createCompoundBaseBean(final HippoContentBean bean, final UserFeedback feedback) {
        final Path javaClass = createJavaClass(bean, feedback);
        JavaSourceUtils.createHippoBean(javaClass, settingsService.getSettings().getSelectedBeansPackage(), bean.getName(), bean.getName());
        JavaSourceUtils.addExtendsClass(javaClass, "HippoCompound");
        JavaSourceUtils.addImport(javaClass, EssentialConst.HIPPO_COMPOUND_IMPORT);
    }


    private void addMethods(final HippoContentBean bean, final Path beanPath, final Collection<String> existing, final UserFeedback feedback, final String imageSetClassName) {
        final List<HippoContentProperty> properties = bean.getProperties();
        for (HippoContentProperty property : properties) {
            final String name = property.getName();
            if (!hasChange(name, existing, beanPath, property.isMultiple(), feedback)) {
                continue;
            }
            String type = property.getType();
            log.debug("processing missing property, BEAN: {}, PROPERTY: {}", bean.getName(), property.getName());

            if (type == null) {
                log.error("Missing type for property, cannot create method {}", property.getName());
                continue;
            }
            if (type.equals("String")) {
                final String cmsType = property.getCmsType();
                if (!Strings.isNullOrEmpty(cmsType) && cmsType.equals(DOCBASE)) {
                    type = DOCBASE;
                }
            }

            final boolean multiple = property.isMultiple();
            String methodName;
            switch (type) {
                case "String":
                case "Html":
                case "Password":
                case "Text":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodString(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;

                case "Date":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodCalendar(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;
                case "Boolean":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodBoolean(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;
                case "Long":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodLong(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;
                case "Double":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodDouble(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;
                case DOCBASE:
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodDocbase(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;
                default:
                    final String message = String.format("TODO: Beanwriter: Failed to create getter for property: %s of type: %s", property.getName(), type);
                    JavaSourceUtils.addClassJavaDoc(beanPath, message);
                    log.warn(message);
                    break;
            }
        }
        //############################################
        // NODE TYPES
        //############################################
        final Path imageSetBeanPath = getBeanPathForImageSet(imageSetClassName);
        final List<HippoContentChildNode> children = bean.getChildren();
        for (HippoContentChildNode child : children) {
            final String name = child.getName();
            if (!hasChange(name, existing, beanPath, child.isMultiple(), feedback)) {
                continue;
            }
            final String type = child.getType();
            log.debug("processing missing node, BEAN: {}, CHILD: {}", bean.getName(), child.getName());

            if (type == null) {
                log.error("Missing type for node, cannot create method {}", child.getName());
                continue;
            }
            final boolean multiple = child.isMultiple();
            String methodName;
            switch (type) {
                case "hippostd:html":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodHippoHtml(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;

                case "hippogallerypicker:imagelink":
                    methodName = GlobalUtils.createMethodName(name);
                    if (imageSetBeanPath == null) {
                        JavaSourceUtils.addBeanMethodImageLink(beanPath, methodName, name, multiple);
                    } else {
                        final String className = JavaSourceUtils.getClassName(imageSetBeanPath);
                        final String importName = JavaSourceUtils.getImportName(imageSetBeanPath);
                        JavaSourceUtils.addBeanMethodInternalImageSet(beanPath, className, importName, methodName, name, multiple);
                    }
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;
                case "hippo:mirror":
                    // TODO: we could add a note to define more specific type instead of HippoBean
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodHippoMirror(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;
                case "hippogallery:image":

                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodHippoImage(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;
                case RESOURCE:
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodHippoResource(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMethodCreated(beanPath, methodName, feedback);
                    break;
                default:
                    // check if project type is used:
                    final String prefix = child.getPrefix();
                    if (prefix.equals(settingsService.getSettings().getProjectNamespace())) {
                        final Map<String, Path> existingBeans = findBeans();
                        for (Map.Entry<String, Path> entry : existingBeans.entrySet()) {
                            final Path myBeanPath = entry.getValue();
                            final HippoEssentialsGeneratedObject a = JavaSourceUtils.getHippoGeneratedAnnotation(myBeanPath);
                            if (a != null && a.getInternalName().equals(type)) {
                                final String className = JavaSourceUtils.getClassName(myBeanPath);
                                methodName = GlobalUtils.createMethodName(name);
                                final String importPath = JavaSourceUtils.getImportName(myBeanPath);
                                JavaSourceUtils.addBeanMethodInternalType(beanPath, className, importPath, methodName, name, multiple);
                                logMethodCreated(beanPath, methodName, feedback);
                                return;
                            }
                        }
                    }
                    final String message = String.format("TODO: Beanwriter: Failed to create getter for node type: %s", type);
                    JavaSourceUtils.addClassJavaDoc(beanPath, message);
                    log.warn(message);
                    break;
            }
        }
    }



    @Override
    public Map<String, Path> getExistingImageTypes() {
        final Map<String, Path> existing = findBeans();
        final Map<String, Path> imageTypes = new HashMap<>();
        imageTypes.put(HIPPO_GALLERY_IMAGE_SET_CLASS, null);
        for (Path path : existing.values()) {
            final String myClass = JavaSourceUtils.getClassName(path);
            final String extendsClass = JavaSourceUtils.getExtendsClass(path);
            if (HIPPO_GALLERY_IMAGE_SET_CLASS.equals(extendsClass)) {
                imageTypes.put(myClass, path);
            }
        }
        return imageTypes;
    }

    private Path getBeanPathForImageSet(final String imageSetClassName) {
        final Map<String, Path> existing = findBeans();
        for (Path path : existing.values()) {
            final String className = JavaSourceUtils.getClassName(path);
            if (className.equals(imageSetClassName)) {
                return path;
            }
        }
        return null;
    }

    @Override
    public void convertImageMethodsForClassname(final String classname, final UserFeedback feedback) {
        if (Strings.isNullOrEmpty(classname)) {
            return;
        }

        final String jcrName = jcrNameForClassName(classname);
        if (jcrName != null) {
            convertImageMethods(jcrName, feedback);
        } else {
            feedback.addError("Could not find selected Image Set: " + classname);
        }
    }

    private String jcrNameForClassName(final String className) {
        if (className.equals(HIPPO_GALLERY_IMAGE_SET_BEAN)
                || className.equals(HIPPO_GALLERY_IMAGE_SET_CLASS)) {
            return className; // ContentBeansService API accepts classname as jcrname for build-in image sets (weird!)
        }

        final Map<String, Path> existingImageTypes = getExistingImageTypes();
        final Path customImageSetBeanPath = existingImageTypes.get(className);
        if (customImageSetBeanPath != null) {
            final HippoEssentialsGeneratedObject annotation = JavaSourceUtils.getHippoGeneratedAnnotation(customImageSetBeanPath);
            if (annotation != null && !Strings.isNullOrEmpty(annotation.getInternalName())) {
                return annotation.getInternalName();
            }
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void convertImageMethods(final String jcrName, final UserFeedback feedback) {
        final Map<String, Path> existing = findBeans();
        final Map<String, String> imageTypes = new HashMap<>();
        final Set<Path> imageTypePaths = new HashSet<>();
        imageTypes.put(HIPPO_GALLERY_IMAGE_SET_CLASS, "org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet");
        imageTypes.put(HIPPO_GALLERY_IMAGE_SET_BEAN, "org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean");
        String newReturnType = null;
        for (Path path : existing.values()) {
            final String myClass = JavaSourceUtils.getClassName(path);
            final String extendsClass = JavaSourceUtils.getExtendsClass(path);
            final HippoEssentialsGeneratedObject annotation = JavaSourceUtils.getHippoGeneratedAnnotation(path);
            if (!Strings.isNullOrEmpty(extendsClass) && extendsClass.equals(HIPPO_GALLERY_IMAGE_SET_CLASS)) {
                imageTypes.put(myClass, JavaSourceUtils.getImportName(path));
                imageTypePaths.add(path);
            }
            if (annotation != null && jcrName.equals(annotation.getInternalName())) {
                newReturnType = myClass;
            }
        }
        if (jcrName.equals(HIPPO_GALLERY_IMAGE_SET_BEAN) || jcrName.equals(HIPPO_GALLERY_IMAGE_SET_CLASS)) {
            newReturnType = HIPPO_GALLERY_IMAGE_SET_CLASS;
        }
        if (Strings.isNullOrEmpty(newReturnType)) {
            log.warn("Could not find return type for image set namespace: {}", jcrName);
            return;
        }
        log.info("Converting existing image beans to new type: {}", newReturnType);
        for (Map.Entry<String, Path> entry : existing.entrySet()) {
            // check if image type and skip if so:
            final Path path = entry.getValue();
            if (imageTypePaths.contains(path)) {
                continue;
            }
            final ExistingMethodsVisitor methods = JavaSourceUtils.getMethodCollection(path);

            final List<EssentialsGeneratedMethod> generatedMethods = methods.getGeneratedMethods();
            final HippoEssentialsGeneratedObject classAnnotation = JavaSourceUtils.getHippoGeneratedAnnotation(path);
            final boolean allowClassUpdate = classAnnotation != null && classAnnotation.isAllowModifications();
            for (EssentialsGeneratedMethod m : generatedMethods) {
                final HippoEssentialsGeneratedObject annotation = JavaSourceUtils.getHippoEssentialsAnnotation(path, m.getMethodDeclaration());
                final boolean allowMethodUpdate = annotation != null && annotation.isAllowModifications();

                final Type type = m.getMethodDeclaration().getReturnType2();
                if (type.isSimpleType()) {
                    final SimpleType simpleType = (SimpleType) type;
                    final String returnType = simpleType.getName().getFullyQualifiedName();
                    // check if image type and different than new return type
                    if (imageTypes.containsKey(returnType) && !returnType.equals(newReturnType)) {
                        log.info("Found image type: {}", returnType);
                        if (!allowClassUpdate) {
                            logClassModificationDisabled(path, m.getMethodName(), feedback);
                        } else if (allowMethodUpdate) {
                            updateImageMethod(path, returnType, newReturnType, imageTypes.get(newReturnType), feedback);
                        } else {
                            logMethodModificationDisabled(path, m.getMethodName(), feedback);
                        }
                    }
                } else if (JavaSourceUtils.getParameterizedType(type) != null) {
                    final String returnType = JavaSourceUtils.getParameterizedType(type);
                    if (imageTypes.containsKey(returnType) && !returnType.equals(newReturnType)) {
                        log.info("Found image type: {}", returnType);
                        if (!allowClassUpdate) {
                            logClassModificationDisabled(path, m.getMethodName(), feedback);
                        } else if (allowMethodUpdate) {
                            updateImageMethod(path, returnType, newReturnType, imageTypes.get(newReturnType), feedback);
                        }
                    }
                }
            }
        }
    }


    private void updateImageMethod(final Path path, final String oldReturnType, final String newReturnType, final String importStatement, final UserFeedback feedback) {
        final CompilationUnit deleteUnit = JavaSourceUtils.getCompilationUnit(path);
        final ExistingMethodsVisitor methodCollection = JavaSourceUtils.getMethodCollection(path);
        final List<EssentialsGeneratedMethod> generatedMethods = methodCollection.getGeneratedMethods();
        final Map<String, EssentialsGeneratedMethod> deletedMethods = new HashMap<>();

        deleteUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                final String methodName = node.getName().getFullyQualifiedName();
                final Type type = node.getReturnType2();
                if (type.isSimpleType()) {
                    final SimpleType simpleType = (SimpleType) type;
                    final String returnTypeName = simpleType.getName().getFullyQualifiedName();
                    final EssentialsGeneratedMethod method = JavaSourceUtils.extractMethod(methodName, generatedMethods);
                    if (method == null) {
                        return super.visit(node);
                    }
                    if (returnTypeName.equals(oldReturnType)) {
                        node.delete();
                        deletedMethods.put(method.getMethodName(), method);
                        return super.visit(node);
                    }
                } else if (JavaSourceUtils.getParameterizedType(type) != null) {
                    final String returnTypeName = JavaSourceUtils.getParameterizedType(type);
                    final EssentialsGeneratedMethod method = JavaSourceUtils.extractMethod(methodName, generatedMethods);
                    if (method == null) {
                        return super.visit(node);
                    }
                    if (returnTypeName == null) {
                        return super.visit(node);
                    }
                    if (returnTypeName.equals(oldReturnType)) {
                        node.delete();
                        deletedMethods.put(method.getMethodName(), method);
                        return super.visit(node);
                    }

                }
                return super.visit(node);
            }
        });
        if (deletedMethods.size() > 0) {
            final AST deleteAst = deleteUnit.getAST();
            final String deletedSource = JavaSourceUtils.rewrite(deleteUnit, deleteAst);
            GlobalUtils.writeToFile(deletedSource, path);
            for (Map.Entry<String, EssentialsGeneratedMethod> entry : deletedMethods.entrySet()) {
                final EssentialsGeneratedMethod oldMethod = entry.getValue();
                // Add replacement methods:
                if (newReturnType.equals(HIPPO_GALLERY_IMAGE_SET_CLASS) || newReturnType.equals(HIPPO_GALLERY_IMAGE_SET_BEAN)) {
                    JavaSourceUtils.addBeanMethodHippoImageSet(path, oldMethod.getMethodName(), oldMethod.getInternalName(), oldMethod.isMultiType());
                } else {
                    JavaSourceUtils.addBeanMethodInternalImageSet(path, newReturnType, importStatement, oldMethod.getMethodName(), oldMethod.getInternalName(), oldMethod.isMultiType());
                }
                logMethodModified(path, oldMethod.getMethodName(), feedback);
            }
        }
    }


    /**
     * Create a bean for giving parent bean path
     *
     * @param bean       none existing bean
     * @param parentPath existing parent bean
     */
    private void createBean(final HippoContentBean bean, final Path parentPath, final UserFeedback feedback) {
        final Path javaClass = createJavaClass(bean, feedback);
        JavaSourceUtils.createHippoBean(javaClass, settingsService.getSettings().getSelectedBeansPackage(), bean.getName(), bean.getName());
        final String extendsName = FilenameUtils.removeExtension(parentPath.toFile().getName());
        JavaSourceUtils.addExtendsClass(javaClass, extendsName);

    }


    private Path createJavaClass(final HippoContentBean bean, final UserFeedback feedback) {
        String name = bean.getName();
        if (name.indexOf(',') != -1) {
            name = name.split(",")[0];
        }
        final String className = GlobalUtils.createClassName(name);
        final Path path = JavaSourceUtils.createJavaClass(projectService.getBeansRootPath().toString(), className,
                settingsService.getSettings().getSelectedBeansPackage(), null);
        logClassCreated(path, className, feedback);
        return path;
    }


    private boolean hasChange(final String name, final Collection<String> existing, final Path beanPath, final boolean multiple, final UserFeedback feedback) {
        if (existing.contains(name)) {
            log.debug("Property already exists {}. Checking if method signature has changed e.g. single value to multiple", name);
            final ExistingMethodsVisitor methodCollection = JavaSourceUtils.getMethodCollection(beanPath);
            final HippoEssentialsGeneratedObject classAnnotation = JavaSourceUtils.getHippoGeneratedAnnotation(beanPath);
            final boolean allowClassUpdate = classAnnotation != null && classAnnotation.isAllowModifications();

            final List<EssentialsGeneratedMethod> generatedMethods = methodCollection.getGeneratedMethods();
            for (EssentialsGeneratedMethod generatedMethod : generatedMethods) {
                final HippoEssentialsGeneratedObject annotation = JavaSourceUtils.getHippoEssentialsAnnotation(beanPath, generatedMethod.getMethodDeclaration());
                final boolean allowMethodUpdate = annotation != null && annotation.isAllowModifications();
                final String internalName = generatedMethod.getInternalName();
                if (name.equals(internalName)) {
                    // check if single/multiple  changed:
                    if (generatedMethod.isMultiType() != multiple) {
                        log.debug("Property changed (single/multiple): trying to update method '{}' of bean '{}'.",
                                generatedMethod.getMethodName(), beanPath);
                        if (!allowClassUpdate) {
                            logClassModificationDisabled(beanPath, generatedMethod.getMethodName(), feedback);
                            return false;
                        }
                        if (allowMethodUpdate) {
                            log.info("Property changed (single/multiple): {}", internalName);
                            return JavaSourceUtils.deleteMethod(generatedMethod, beanPath);
                        } else {
                            logMethodModificationDisabled(beanPath, generatedMethod.getMethodName(), feedback);
                            return false;
                        }
                    }
                    // TODO: check check if signature changed:
                }
            }
            return false;
        }
        return true;
    }

    private String getBaseSupertype() {
        return settingsService.getSettings().getProjectNamespace() + ":basedocument";
    }

    private void logClassCreated(final Path beanPath, final String className, final UserFeedback feedback) {
        final String message = String.format("Created HST bean '%s' (%s).", className, beanPath);
        logUserFeedback(message, true, feedback);
    }

    private void logMethodCreated(final Path beanPath, final String methodName, final UserFeedback feedback) {
        final String message = String.format("Created method: '%s' in HST bean: '%s'.", methodName, beanPath);
        logUserFeedback(message, true, feedback);
    }

    private void logMethodModified(final Path beanPath, final String methodName, final UserFeedback feedback) {
        final String message = String.format("Re-created method '%s' in HST bean '%s'.", methodName, beanPath);
        logUserFeedback(message, true, feedback);
    }

    private void logMethodDeleted(final Path beanPath, final String methodName, final UserFeedback feedback) {
        final String message = String.format("Deleted method '%s' in HST bean '%s'.", methodName, beanPath);
        logUserFeedback(message, true, feedback);
    }

    private void logMethodModificationDisabled(final Path beanPath, final String methodName, final UserFeedback feedback) {
        final String message = String.format("Return type of method '%s' in bean '%s' should be changed, " +
                "but modifications are disabled at method level.", methodName, beanPath);
        logUserFeedback(message, false, feedback);
    }

    private void logClassModificationDisabled(final Path beanPath, final String methodName, final UserFeedback feedback) {
        final String message = String.format("Return type of method '%s' in bean '%s' should be changed, " +
                "but modifications are disabled at the class level.", methodName, beanPath);
        logUserFeedback(message, false, feedback);
    }

    private void logUserFeedback(final String message, final boolean success, final UserFeedback feedback) {
        if (success) {
            feedback.addSuccess(message);
            log.info("@@@ {}", message);
        } else {
            feedback.addError(message);
            log.warn("@@@ {}", message);
        }
    }
}

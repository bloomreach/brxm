/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.services;

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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.ActionType;
import org.onehippo.cms7.essentials.dashboard.model.BeanWriterLogEntry;
import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoContentBean;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoContentChildNode;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoContentProperty;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoEssentialsGeneratedObject;
import org.onehippo.cms7.essentials.dashboard.utils.code.EssentialsGeneratedMethod;
import org.onehippo.cms7.essentials.dashboard.utils.code.ExistingMethodsVisitor;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class ContentBeansService {

    public static final String HIPPO_GALLERY_IMAGE_SET_BEAN = "HippoGalleryImageSetBean";
    public static final String HIPPO_GALLERY_IMAGE_SET_CLASS = "HippoGalleryImageSet";
    public static final String RELATED_MIXIN = "relateddocs:relatabledocs";
    public static final String DOCBASE = "Docbase";
    public static final String RESOURCE = "hippo:resource";
    private static Logger log = LoggerFactory.getLogger(ContentBeansService.class);

    private final PluginContext context;
    private final String baseSupertype;
    private static final String BASE_COMPOUND_TYPE = "hippo:compound";
    public static final String MSG_ADDED_METHOD = "@@@ added [{}] method";
    public static final String CONTEXT_BEAN_DATA = BeanWriterUtils.class.getName();
    public static final String CONTEXT_BEAN_IMAGE_SET = BeanWriterUtils.class.getName() + "imageset";

    private final Set<HippoContentBean> contentBeans;
    /**
     * How many loops we run (beans extending none existing beans)
     */
    private static final int MISSING_DEPTH_MAX = 5;
    private int missingBeansDepth = 0;

    public ContentBeansService(final PluginContext context) {
        this.context = context;
        this.baseSupertype = context.getProjectNamespacePrefix() + ':' + "basedocument";
        this.contentBeans = getContentBeans();
    }


    public void createBeans() throws RepositoryException {

        final Map<String, Path> existing = findExistingBeans();
        final List<HippoContentBean> missingBeans = Lists.newArrayList(filterMissingBeans(existing));
        final Iterator<HippoContentBean> missingBeanIterator = missingBeans.iterator();
        for (; missingBeanIterator.hasNext(); ) {
            final HippoContentBean missingBean = missingBeanIterator.next();
            // check if directly extending compound:
            final Set<String> superTypes = missingBean.getSuperTypes();
            if (superTypes.size() == 1 && superTypes.iterator().next().equals(BASE_COMPOUND_TYPE)) {
                createCompoundBaseBean(missingBean);
                missingBeanIterator.remove();
            } else {
                final String parent = findExistingParent(missingBean, existing);
                if (parent != null) {
                    log.debug("found parent: {}, {}", parent, missingBean);
                    missingBeanIterator.remove();
                    final Path parentPath = existing.get(parent);
                    if (parentPath == null) {
                        log.error("Couldn't find parent bean for: {}", parent);
                        if (parent.equals(baseSupertype)) {
                            log.error("Base document type is missing: {}", parent);
                        }
                        continue;
                    }
                    createBean(missingBean, parentPath);
                }
            }
        }
        // process beans without resolved parent beans
        processMissing(missingBeans);
        processProperties();
        // check if still missing(beans that extended missing beans)
        final Iterator<HippoContentBean> extendedMissing = filterMissingBeans(findExistingBeans());
        final boolean hasNonCreatedBeans = extendedMissing.hasNext();
        if (missingBeansDepth < MISSING_DEPTH_MAX && hasNonCreatedBeans) {
            missingBeansDepth++;
            createBeans();
        } else if (hasNonCreatedBeans) {
            log.error("Not all beans were created: {}", extendedMissing);
        }
        processRelatedDocuments();

    }


    /**
     * Removes methods which are annotated but missing within content services
     */
    public void cleanupMethods() {
        final Set<HippoContentBean> beans = getContentBeans();
        final Map<String, Path> existing = findExistingBeans();
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
                        if (!allowClassUpdate) {
                            context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry("Method should be deleted, but modifications are disabled on the Bean class level: "
                                    + internalName + " for class at " + path.toString()));
                        } else if (allowMethodUpdate) {
                            log.info("@Missing declaration for: {}. Method will be deleted", internalName);
                            final boolean deleted = JavaSourceUtils.deleteMethod(method, path);
                            final String methodName = method.getMethodName();
                            if (deleted) {
                                context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(path.toString(), methodName, ActionType.DELETED_METHOD));
                            } else {
                                context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(String.format("Failed to  delete method: %s", methodName)));
                            }
                        } else {
                            context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry("Method should be removed, but modifications are disabled: "
                                    + method.getInternalName() + ", " + method.getMethodName()));
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


    private void processRelatedDocuments() {
        boolean hasRelatedDocs = false;
        for (HippoContentBean contentBean : contentBeans) {
            if (contentBean.getContentType().getAggregatedTypes().contains(RELATED_MIXIN)) {
                hasRelatedDocs = true;
            }
        }
        if (!hasRelatedDocs) {
            return;
        }

        Map<String, Path> existing = findExistingBeans();
        for (HippoContentBean contentBean : contentBeans) {
            final Path path = existing.get(contentBean.getName());
            if (path != null && contentBean.getContentType().getAggregatedTypes().contains(RELATED_MIXIN)) {
                final ExistingMethodsVisitor methodCollection = JavaSourceUtils.getMethodCollection(path);
                final Set<String> methodInternalNames = methodCollection.getMethodInternalNames();
                if (!methodInternalNames.contains(EssentialConst.RELATEDDOCS_DOCS)) {
                    log.debug("Adding related docs method to: ", path);
                    JavaSourceUtils.addRelatedDocsMethod(EssentialConst.METHOD_RELATED_DOCUMENTS, path);
                    log.debug(MSG_ADDED_METHOD, EssentialConst.METHOD_RELATED_DOCUMENTS);
                    context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(path.toString(), EssentialConst.METHOD_RELATED_DOCUMENTS, ActionType.CREATED_METHOD));
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


    private Iterator<HippoContentBean> filterMissingBeans(final Map<String, Path> existing) {
        final Iterable<HippoContentBean> missingBeans = Iterables.filter(contentBeans, new Predicate<HippoContentBean>() {
            @Override
            public boolean apply(HippoContentBean b) {
                return !existing.containsKey(b.getName());
            }
        });
        // process beans with known (project) supertypes:
        return Lists.newArrayList(missingBeans).iterator();
    }


    private void processProperties() {
        final Map<String, Path> existing = findExistingBeans();
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
                    addMethods(bean, beanPath, existingMethods);
                } else {
                    final ExistingMethodsVisitor ownMethodCollection = JavaSourceUtils.getMethodCollection(beanPath);
                    addMethods(bean, beanPath, ownMethodCollection.getMethodInternalNames());
                }

            }
        }
    }


    private void processMissing(final List<HippoContentBean> missingBeans) {
        for (HippoContentBean missingBean : missingBeans) {
            final SortedSet<String> mySupertypes = missingBean.getContentType().getSuperTypes();
            if (mySupertypes.contains("hippogallery:relaxed")) {
                final Path javaClass = createJavaClass(missingBean);
                JavaSourceUtils.createHippoBean(javaClass, context.beansPackageName(), missingBean.getName(), missingBean.getName());
                JavaSourceUtils.addExtendsClass(javaClass, HIPPO_GALLERY_IMAGE_SET_CLASS);
                JavaSourceUtils.addImport(javaClass, EssentialConst.HIPPO_IMAGE_SET_IMPORT);
                addMethods(missingBean, javaClass, new ArrayList<>());
            }
        }
    }

    private String findExistingParent(final HippoContentBean missingBean, final Map<String, Path> existing) {
        final Set<String> superTypes = missingBean.getSuperTypes();
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


    public final Set<HippoContentBean> getContentBeans() {
        try {
            final Set<HippoContentBean> beans = new HashSet<>();
            final Set<ContentType> projectContentTypes = getProjectContentTypes();
            for (ContentType projectContentType : projectContentTypes) {
                final HippoContentBean bean = new HippoContentBean(context, projectContentType);
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
    public Set<ContentType> getProjectContentTypes() throws RepositoryException {
        final String namespacePrefix = context.getProjectNamespacePrefix();
        final Set<ContentType> projectContentTypes = new HashSet<>();
        final Session session = context.createSession();
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
            GlobalUtils.cleanupSession(session);
        }
        return Collections.emptySet();
    }


    //############################################
    // UTILS
    //############################################


    private Map<String, Path> findExistingBeans() {
        final Path startDir = context.getBeansPackagePath();
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


    private void createCompoundBaseBean(final HippoContentBean bean) {
        final Path javaClass = createJavaClass(bean);
        JavaSourceUtils.createHippoBean(javaClass, context.beansPackageName(), bean.getName(), bean.getName());
        JavaSourceUtils.addExtendsClass(javaClass, "HippoCompound");
        JavaSourceUtils.addImport(javaClass, EssentialConst.HIPPO_COMPOUND_IMPORT);
    }


    private void addMethods(final HippoContentBean bean, final Path beanPath, final Collection<String> existing) {
        final List<HippoContentProperty> properties = bean.getProperties();
        for (HippoContentProperty property : properties) {
            final String name = property.getName();
            if (!hasChange(name, existing, beanPath, property.isMultiple())) {
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
                    logMessage(beanPath, methodName);
                    break;

                case "Date":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodCalendar(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMessage(beanPath, methodName);
                    break;
                case "Boolean":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodBoolean(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMessage(beanPath, methodName);
                    break;
                case "Long":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodLong(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMessage(beanPath, methodName);
                    break;
                case "Double":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodDouble(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMessage(beanPath, methodName);
                    break;
                case DOCBASE:
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodDocbase(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMessage(beanPath, methodName);
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
        final List<HippoContentChildNode> children = bean.getChildren();
        for (HippoContentChildNode child : children) {
            final String name = child.getName();
            if (!hasChange(name, existing, beanPath, child.isMultiple())) {
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
                    logMessage(beanPath, methodName);
                    break;

                case "hippogallerypicker:imagelink":
                    methodName = GlobalUtils.createMethodName(name);
                    final Path path = extractPath();
                    if (path == null) {
                        JavaSourceUtils.addBeanMethodImageLink(beanPath, methodName, name, multiple);
                    } else {
                        final String className = JavaSourceUtils.getClassName(path);
                        final String importName = JavaSourceUtils.getImportName(path);
                        JavaSourceUtils.addBeanMethodInternalImageSet(beanPath, className, importName, methodName, name, multiple);
                    }
                    existing.add(name);
                    logMessage(beanPath, methodName);
                    break;
                case "hippo:mirror":
                    // TODO: we could add a note to define more specific type instead of HippoBean
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodHippoMirror(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMessage(beanPath, methodName);
                    break;
                case "hippogallery:image":

                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodHippoImage(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMessage(beanPath, methodName);
                    break;
                case RESOURCE:
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodHippoResource(beanPath, methodName, name, multiple);
                    existing.add(name);
                    logMessage(beanPath, methodName);
                    break;
                default:
                    // check if project type is used:
                    final String prefix = child.getPrefix();
                    if (prefix.equals(context.getProjectNamespacePrefix())) {
                        final Map<String, Path> existingBeans = findExistingBeans();
                        for (Map.Entry<String, Path> entry : existingBeans.entrySet()) {
                            final Path myBeanPath = entry.getValue();
                            final HippoEssentialsGeneratedObject a = JavaSourceUtils.getHippoGeneratedAnnotation(myBeanPath);
                            if (a != null && a.getInternalName().equals(type)) {
                                final String className = JavaSourceUtils.getClassName(myBeanPath);
                                methodName = GlobalUtils.createMethodName(name);
                                final String importPath = JavaSourceUtils.getImportName(myBeanPath);
                                JavaSourceUtils.addBeanMethodInternalType(beanPath, className, importPath, methodName, name, multiple);
                                context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
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


    private void logMessage(final Path beanPath, final String methodName) {
        context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
        log.debug(MSG_ADDED_METHOD, methodName);
    }


    private Path extractPath() {
        final Multimap<String, Object> pluginContextData = context.getPluginContextData();
        final Collection<Object> data = pluginContextData.get(CONTEXT_BEAN_IMAGE_SET);
        Path path = null;
        if (data != null && data.size() == 1) {
            final Object next = data.iterator().next();
            if (next instanceof Path) {
                path = (Path) next;
            }
        }
        return path;
    }


    public Map<String, Path> getExistingImageTypes() {
        final Map<String, Path> existing = findExistingBeans();
        final Map<String, Path> imageTypes = new HashMap<>();
        imageTypes.put(HIPPO_GALLERY_IMAGE_SET_CLASS, null);
        for (Path path : existing.values()) {
            final String myClass = JavaSourceUtils.getClassName(path);
            final String extendsClass = JavaSourceUtils.getExtendsClass(path);
            if (!Strings.isNullOrEmpty(extendsClass) && extendsClass.equals(HIPPO_GALLERY_IMAGE_SET_CLASS)) {
                imageTypes.put(myClass, path);
            }

        }
        return imageTypes;
    }


    @SuppressWarnings("rawtypes")
    public void convertImageMethods(final String newImageNamespace) {
        final Map<String, Path> existing = findExistingBeans();
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
            if (annotation != null && newImageNamespace.equals(annotation.getInternalName())) {
                newReturnType = myClass;
            }
        }
        if (newImageNamespace.equals(HIPPO_GALLERY_IMAGE_SET_BEAN) || newImageNamespace.equals(HIPPO_GALLERY_IMAGE_SET_CLASS)) {
            newReturnType = HIPPO_GALLERY_IMAGE_SET_CLASS;
        }
        if (Strings.isNullOrEmpty(newReturnType)) {
            log.warn("Could not find return type for image set namespace: {}", newImageNamespace);
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
                            context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry("Method return type should be changed, but modifications are disabled at the Bean class level: "
                                    + m.getInternalName() + " in the class at " + path.toString()));
                        } else if (allowMethodUpdate) {
                            updateImageMethod(path, returnType, newReturnType, imageTypes.get(newReturnType));
                        } else {
                            context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry("Method return type should be changed, but modifications are disabled: "
                                    + m.getInternalName() + ", " + m.getMethodName()));
                        }
                    }
                } else if (JavaSourceUtils.getParameterizedType(type) != null) {
                    final String returnType = JavaSourceUtils.getParameterizedType(type);
                    if (imageTypes.containsKey(returnType) && !returnType.equals(newReturnType)) {
                        log.info("Found image type: {}", returnType);
                        if (!allowClassUpdate) {
                            context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry("Method return type should be changed, but modifications are disabled at the Bean class level: "
                                    + m.getInternalName() + " in the class at " + path.toString()));
                        } else if (allowMethodUpdate) {
                            updateImageMethod(path, returnType, newReturnType, imageTypes.get(newReturnType));
                        }
                    }
                }
            }
        }
    }


    private void updateImageMethod(final Path path, final String oldReturnType, final String newReturnType, final String importStatement) {
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
                log.debug("Replaced old method: {} with new return type: {}", oldMethod.getMethodName(), newReturnType);
                context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(path.toString(), oldMethod.getMethodName(), ActionType.MODIFIED_METHOD));
            }
        }
    }


    /**
     * Create a bean for giving parent bean path
     *
     * @param bean       none existing bean
     * @param parentPath existing parent bean
     */
    private void createBean(final HippoContentBean bean, final Path parentPath) {
        final Path javaClass = createJavaClass(bean);
        JavaSourceUtils.createHippoBean(javaClass, context.beansPackageName(), bean.getName(), bean.getName());
        final String extendsName = FilenameUtils.removeExtension(parentPath.toFile().getName());
        JavaSourceUtils.addExtendsClass(javaClass, extendsName);

    }


    private Path createJavaClass(final HippoContentBean bean) {
        String name = bean.getName();
        if (name.indexOf(',') != -1) {
            name = name.split(",")[0];
        }
        final String className = GlobalUtils.createClassName(name);
        final Path path = JavaSourceUtils.createJavaClass(context.getBeansRootPath().toString(), className, context.beansPackageName(), null);
        context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(ActionType.CREATED_CLASS, path.toString(), className));

        return path;
    }


    private boolean hasChange(final String name, final Collection<String> existing, final Path beanPath, final boolean multiple) {
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
                        if (!allowClassUpdate) {
                            // there was a change, however, class is marked as read only
                            log.warn("Property changed (single/multiple): {}, but bean class changes not allowed", internalName);
                            context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry("Method return type should be changed, but modifications are disabled on the Bean class level: "
                                    + generatedMethod.getMethodName() + " for class at " + beanPath.toString()));
                            return false;
                        }
                        if (allowMethodUpdate) {
                            log.info("Property changed (single/multiple): {}", internalName);
                            return JavaSourceUtils.deleteMethod(generatedMethod, beanPath);
                        } else {
                            // there was a change, however, method is marked as read only
                            log.warn("Property changed (single/multiple): {}, but changes not allowed", internalName);
                            context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry("Method return type should be changed, but modifications are disabled: "
                                    + internalName + ", " + generatedMethod.getMethodName()));

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


}

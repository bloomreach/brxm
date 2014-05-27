/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FilenameUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoContentBean;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoContentProperty;
import org.onehippo.cms7.essentials.dashboard.utils.code.ExistingMethodsVisitor;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.HippoContentTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * @version "$Id$"
 */
public class ContentBeansService {

    private static Logger log = LoggerFactory.getLogger(ContentBeansService.class);

    private final PluginContext context;
    private final String baseSupertype;
    private static final String BASE_TYPE = "hippo:document";


    public ContentBeansService(final PluginContext context) {
        this.context = context;
        this.baseSupertype = context.getProjectNamespacePrefix() + ':' + "basedocument";
    }

    public void createBeans() throws RepositoryException {
        final Set<HippoContentBean> contentBeans = getContentBeans();
        final Map<String, Path> existing = findExitingBeans();
        final Iterable<HippoContentBean> missingBeans = Iterables.filter(contentBeans, new Predicate<HippoContentBean>() {
            @Override
            public boolean apply(HippoContentBean b) {
                return !existing.containsKey(b.getName());
            }
        });

        // process beans with known supertypes:
        final Iterator<HippoContentBean> iterator = Lists.newArrayList(missingBeans).iterator();
        while (iterator.hasNext()) {
            final HippoContentBean next = iterator.next();
            final String parent = findExistingParent(next, existing);
            if (parent != null) {
                log.info("found parent: {}, {}", parent, next);
                iterator.remove();
                createBean(next, existing.get(parent));
            }
        }

        for (HippoContentBean missingBean : missingBeans) {
            log.info("missingBean {}", missingBean);
        }

    }

    private String findExistingParent(final HippoContentBean missingBean, final Map<String, Path> existing) {
        final Set<String> superTypes = missingBean.getSuperTypes();
        if (superTypes.size() == 1 && superTypes.iterator().next().equals(baseSupertype)) {
            return baseSupertype;
        }
        for (String superType : superTypes) {
            if (!superType.equals(baseSupertype) && existing.containsKey(superType)) {
                // TODO improve nested types
                return superType;
            }
        }
        return null;
    }


    public Set<HippoContentBean> getContentBeans() throws RepositoryException {
        final Set<HippoContentBean> beans = new HashSet<>();
        final Set<ContentType> projectContentTypes = getProjectContentTypes();
        for (ContentType projectContentType : projectContentTypes) {
            final HippoContentBean bean = new HippoContentBean(context, projectContentType);
            beans.add(bean);
        }
        return beans;
    }


    /**
     * Fetch project content types
     *
     * @param context instance of PluginContext
     * @return empty collection if no types are found
     * @throws javax.jcr.RepositoryException
     */
    public Set<ContentType> getProjectContentTypes() throws RepositoryException {
        final String namespacePrefix = context.getProjectNamespacePrefix();
        final Set<ContentType> projectContentTypes = new HashSet<>();
        // TODO disable
        final Session session = getSession();//context.createSession();
        try {
            final ContentTypeService service = new HippoContentTypeService(session);
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

    private Session getSession() throws RepositoryException {
        final HippoRepository hippoRepository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        return hippoRepository.login("admin", "admin".toCharArray());
    }

    private Map<String, Path> findExitingBeans() {
        // TODO: mm remove hack
        final Path startDir = new File("/home/machak/java/projects/hippo/appstore/site/src/main/java/org/example/beans").toPath();///context.getBeansPackagePath();
        final Map<String, Path> existingBeans = new HashMap<>();
        final List<Path> directories = new ArrayList<>();
        GlobalUtils.populateDirectories(startDir, directories);
        final String pattern = "*.java";
        for (Path directory : directories) {
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pattern)) {
                for (Path path : stream) {
                    final String nodeJcrType = JavaSourceUtils.getNodeJcrType(path);
                    if (nodeJcrType != null) {
                        log.info("nodeJcrType {}", nodeJcrType);
                        existingBeans.put(nodeJcrType, path);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading java files", e);
            }
        }
        return existingBeans;

    }

    /**
     * Create a bean for giving parent bean path
     *
     * @param bean       none existing bean
     * @param parentPath existing parent bean
     */
    private void createBean(final HippoContentBean bean, final Path parentPath) {
        // create our bean:
        final Path startDir = new File("/home/machak/java/projects/hippo/appstore/site/src/main/java/org/example/beans").toPath();
        final String className = GlobalUtils.createClassName(bean.getName());
        final Path javaClass = JavaSourceUtils.createJavaClass("/home/machak/java/projects/hippo/appstore/site/src/main/java/", className, "org.example.beans", null);
        final String hippoBean = JavaSourceUtils.createHippoBean(javaClass, "org.example.beans", bean.getName(), bean.getName());
        final String extendsName = FilenameUtils.removeExtension(parentPath.toFile().getName());
        JavaSourceUtils.addExtendsClass(javaClass, extendsName);
        JavaSourceUtils.addImport(javaClass, EssentialConst.HIPPO_DOCUMENT_IMPORT);

        log.info("hippoBean {}", hippoBean);
        final String name = bean.getName();

        final ExistingMethodsVisitor methodCollection = JavaSourceUtils.getMethodCollection(parentPath);
        final Set<String> existing = methodCollection.getMethodInternalNames();
        final List<HippoContentProperty> properties = bean.getProperties();
        for (HippoContentProperty property : properties) {
              if(!existing.contains(property.getName())){
                  log.info("property {}", property);
              }
        }



        //final String className = GlobalUtils.createClassName(name);
        //final Path javaClass = JavaSourceUtils.createJavaClass(context.getSiteJavaRoot(), className, context.beansPackageName(), null);
        //JavaSourceUtils.addExtendsClass(javaClass, extendsName);
        //JavaSourceUtils.createHippoBean(javaClass, context.beansPackageName(), name, name);
    }


}

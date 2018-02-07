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

package org.onehippo.cms7.essentials.plugin.restservices;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Strings;

import org.apache.commons.lang.BooleanUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.annotations.AnnotationUtils;
import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.PlaceholderService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.service.RebuildService;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/restservices")
public class RestPluginResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestPluginResource.class);

    private final RebuildService rebuildService;
    private final JcrService jcrService;
    private final SettingsService settingsService;
    private final ProjectService projectService;
    private final PlaceholderService placeholderService;

    @Inject
    public RestPluginResource(final RebuildService rebuildService, final JcrService jcrService,
                              final SettingsService settingsService, final ProjectService projectService,
                              final PlaceholderService placeholderService) {
        this.rebuildService = rebuildService;
        this.jcrService = jcrService;
        this.settingsService = settingsService;
        this.projectService = projectService;
        this.placeholderService = placeholderService;
    }

    @POST
    @Path("/")
    public UserFeedback setup(final Map<String, Object> parameters, @Context HttpServletResponse response) {
        final UserFeedback feedback = new UserFeedback();
        final boolean isGenericApiEnabled = BooleanUtils.isTrue((Boolean) parameters.get(RestPluginConst.GENERIC_API_ENABLED));
        final boolean isManualApiEnabled = BooleanUtils.isTrue((Boolean) parameters.get(RestPluginConst.MANUAL_API_ENABLED));
        final String genericRestName = (String) parameters.get(RestPluginConst.GENERIC_REST_NAME);
        final String manualRestName = (String) parameters.get(RestPluginConst.MANUAL_REST_NAME);

        if (isGenericApiEnabled && isManualApiEnabled && genericRestName.equals(manualRestName)) {
            return feedback.addError("Generic and manual REST resources must use different URLs.");
        }

        if (isManualApiEnabled) {
            if (Strings.isNullOrEmpty(manualRestName)) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                return feedback.addError("Manual REST resource URL must be specified.");
            }

            final Set<ValidBean> validBeans = annotateBeans((List<String>) parameters.get(RestPluginConst.JAVA_FILES));
            final Map<String, Object> placeholderData = placeholderService.makePlaceholders();

            for (ValidBean validBean : validBeans) {
                placeholderData.put("beanName", validBean.getBeanName());
                placeholderData.put("fullQualifiedName", validBean.getFullQualifiedName());
                projectService.copyResource("/BeanNameResource.txt",
                        "{{restFolder}}/{{beanName}}Resource.java", placeholderData, false, false);
            }

            placeholderData.put("beans", validBeans);
            if (!projectService.copyResource("/spring-plain-rest-api.xml",
                    "{{siteOverrideFolder}}/spring-plain-rest-api.xml", placeholderData, true, false)) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return feedback.addError("Failed to set up Spring configuration for 'manual' REST endpoint. See back-end logs for mode details.");
            }

            if (!setupPlainRestMount(manualRestName, RestPluginConst.MANUAL_PIPELINE_NAME, feedback)) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return feedback;
            }

            rebuildService.requestRebuild("restServices");
            feedback.addSuccess("Spring configuration changed, project rebuild needed.");
        }

        if (isGenericApiEnabled) {
            if (Strings.isNullOrEmpty(genericRestName)) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                return feedback.addError("Generic REST resource URL must be specified.");
            }

            if (!setupPlainRestMount(genericRestName, RestPluginConst.GENERIC_PIPELINE_NAME, feedback)) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return feedback;
            }
        }

        return feedback.addSuccess("REST configuration setup was successful.");
    }

    private boolean setupPlainRestMount(final String mountName, final String pipelineName, final UserFeedback feedback) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(RestPluginConst.REST_NAME, mountName);
        properties.put(RestPluginConst.PIPELINE_NAME, pipelineName);

        final Session session = jcrService.createSession();
        try {
            final Node targetNode = session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
            jcrService.importResource(targetNode, "/plain_mount.xml", properties);
            session.save();
        } catch (RepositoryException e) {
            LOG.error("Failed to import REST mount point '{}'.", mountName, e);
            feedback.addError("Failed to set up REST endpoint '" + mountName + "'. See back-end logs for mode details.");
            return false;
        } finally {
            jcrService.destroySession(session);
        }
        return true;
    }

    private Set<ValidBean> annotateBeans(final List<String> paths) {
        final Set<ValidBean> validBeans = new HashSet<>();

        for (String path : paths) {
            final File file = new File(path);
            if (file.exists()) {
                final java.nio.file.Path filePath = file.toPath();
                final String className = JavaSourceUtils.getClassName(filePath);
                if (Strings.isNullOrEmpty(className)) {
                    continue;
                }
                String source = GlobalUtils.readTextFile(filePath).toString();
                // access annotation:
                source = AnnotationUtils.addXmlAccessNoneAnnotation(source);
                // root annotation
                source = AnnotationUtils.addXmlRootAnnotation(source, className.toLowerCase());
                // annotate fields:
                source = AnnotationUtils.addXmlElementAnnotation(source);
                //add adapters:
                source = AnnotationUtils.addKnownAdapters(source);
                // rewrite bean:
                GlobalUtils.writeToFile(source, filePath);

                final ValidBean bean = new ValidBean();
                bean.setBeanName(className);
                bean.setBeanPath(path);
                bean.setBeanPackage(JavaSourceUtils.getPackageName(filePath));
                final String qualifiedClassName = JavaSourceUtils.getFullQualifiedClassName(filePath);
                bean.setFullQualifiedName(qualifiedClassName);
                final String resourceName = MessageFormat.format("{0}.{1}Resource", settingsService.getSettings().getSelectedRestPackage(), className);
                bean.setFullQualifiedResourceName(resourceName);
                validBeans.add(bean);
            }
        }
        return validBeans;
    }
}

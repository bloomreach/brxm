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
import java.util.ArrayList;
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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.plugin.sdk.model.UserFeedback;
import org.onehippo.cms7.essentials.plugin.sdk.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.plugin.sdk.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.plugin.sdk.rest.RestfulList;
import org.onehippo.cms7.essentials.plugin.sdk.service.JcrService;
import org.onehippo.cms7.essentials.plugin.sdk.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.service.RebuildService;
import org.onehippo.cms7.essentials.plugin.sdk.service.SettingsService;
import org.onehippo.cms7.essentials.plugin.sdk.services.ContentBeansService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.HstUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.annotations.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/restservices")
public class RestPluginResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestPluginResource.class);

    @Inject private RebuildService rebuildService;
    @Inject private PluginContextFactory contextFactory;
    @Inject private JcrService jcrService;
    @Inject private SettingsService settingsService;
    @Inject private ContentBeansService contentBeansService;
    @Inject private ProjectService projectService;

    @GET
    @Path("/beans")
    public RestfulList<KeyValueRestful> getHippoBeans() {
        final RestfulList<KeyValueRestful> list = new RestfulList<>();
        final Map<String, java.nio.file.Path> beans = contentBeansService.findBeans();
        for (java.nio.file.Path beanPath : beans.values()) {
            final String fileName = beanPath.toFile().getName();
            list.add(new KeyValueRestful(fileName, beanPath.toString()));
        }
        return list;
    }


    @GET
    @Path("/mounts")
    public List<MountRestful> getHippoSites() throws RepositoryException {
        final Set<Node> hstMounts = HstUtils.getHstMounts(jcrService);
        final List<MountRestful> list = new ArrayList<>();
        for (Node m : hstMounts) {
            list.add(new MountRestful(m.getIdentifier(), m.getPath(), m.getName()));
        }
        return list;
    }


    @POST
    @Path("/")
    public UserFeedback executeInstructionPackage(final PostPayloadRestful payloadRestful, @Context HttpServletResponse response) {
        final UserFeedback feedback = new UserFeedback();
        final PluginContext context = contextFactory.getContext();
        final Map<String, String> values = payloadRestful.getValues();
        final boolean isGenericApiEnabled = Boolean.valueOf(values.get(RestPluginConst.GENERIC_API_ENABLED));
        final boolean isManualApiEnabled = Boolean.valueOf(values.get(RestPluginConst.MANUAL_API_ENABLED));
        final String genericRestName = values.get(RestPluginConst.GENERIC_REST_NAME);
        final String manualRestName = values.get(RestPluginConst.MANUAL_REST_NAME);

        if (isGenericApiEnabled && isManualApiEnabled && genericRestName.equals(manualRestName)) {
            return feedback.addError("Generic and manual REST resources must use different URLs.");
        }

        if (isManualApiEnabled) {
            if (Strings.isNullOrEmpty(manualRestName)) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                return feedback.addError("Manual REST resource URL must be specified.");
            }

            final String selectedBeans = values.get(RestPluginConst.JAVA_FILES);
            final Set<ValidBean> validBeans = annotateBeans(selectedBeans);

            for (ValidBean validBean : validBeans) {
                context.addPlaceholderData("beanName", validBean.getBeanName());
                context.addPlaceholderData("fullQualifiedName", validBean.getFullQualifiedName());
                projectService.copyResource("/BeanNameResource.txt",
                        "{{restFolder}}/{{beanName}}Resource.java", context, false, false);
            }

            context.addPlaceholderData("beans", validBeans);
            if (!projectService.copyResource("/spring-plain-rest-api.xml",
                    "{{siteOverrideFolder}}/spring-plain-rest-api.xml", context, true, false)) {
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

    private Set<ValidBean> annotateBeans(final String input) {
        final Set<ValidBean> validBeans = new HashSet<>();

        if (Strings.isNullOrEmpty(input)) {
            return validBeans;
        }
        final Iterable<String> split = Splitter.on(',').split(input);
        for (String path : split) {
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

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

package org.onehippo.cms7.essentials.dashboard.restservices;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instruction.FileInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.PluginInstructionSet;
import org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.packaging.InstructionPackage;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HstUtils;
import org.onehippo.cms7.essentials.dashboard.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/restservices")
public class RestPluginResource extends BaseResource {


    private static final Logger log = LoggerFactory.getLogger(RestPluginResource.class);

    @GET
    @Path("/beans")
    public RestfulList<KeyValueRestful> getHippoBeans(@Context ServletContext servletContext) {

        final RestfulList<KeyValueRestful> list = new RestfulList<>();
        final Map<String, java.nio.file.Path> hippoBeans = BeanWriterUtils.mapExitingBeanNames(getContext(servletContext), "java");
        for (Map.Entry<String, java.nio.file.Path> bean : hippoBeans.entrySet()) {
            list.add(new KeyValueRestful(bean.getKey(), bean.getValue().toString()));
        }
        return list;
    }


    @GET
    @Path("/mounts")
    public List<MountRestful> getHippoSites(@Context ServletContext servletContext) throws RepositoryException {

        final Set<Node> hstMounts = HstUtils.getHstMounts(getContext(servletContext));
        final List<MountRestful> list = new ArrayList<>();
        for (Node m : hstMounts) {
            list.add(new MountRestful(m.getIdentifier(), m.getPath(), m.getName()));
        }
        return list;
    }


    @POST
    @Path("/")
    public MessageRestful executeInstructionPackage(final PostPayloadRestful payloadRestful, @Context ServletContext servletContext) {

        final MessageRestful message = new MessageRestful();

        final Map<String, String> values = payloadRestful.getValues();
        final String restName = values.get(RestPluginConst.REST_NAME);
        final String restType = values.get(RestPluginConst.REST_TYPE);
        final String selectedBeans = values.get(RestPluginConst.JAVA_FILES);
        if (Strings.isNullOrEmpty(restName) || Strings.isNullOrEmpty(restType)) {
            return new ErrorMessageRestful("REST service name / type or both were empty");
        }
        final PluginContext context = getContext(servletContext);

        final InstructionPackage instructionPackage = new RestServicesInstructionPackage();
        // TODO: figure out injection part
        getInjector().autowireBean(instructionPackage);

        final Set<ValidBean> validBeans = annotateBeans(selectedBeans, context);

        instructionPackage.setProperties(new HashMap<String, Object>(values));
        // add beans for instruction set loop:
        instructionPackage.getProperties().put("beans", validBeans);
        instructionPackage.execute(context);
        // create endpoint rest
        if (restType.equals("plain")) {
            final InstructionExecutor executor = new PluginInstructionExecutor();
            for (ValidBean validBean : validBeans) {
                final Map<String, Object> properties = new HashMap<>();
                properties.put("beanPackage", validBean.getBeanPackage());
                properties.put("beanName", validBean.getBeanName());
                properties.put("beans", validBean.getFullQualifiedName());
                properties.put("fullQualifiedName", validBean.getFullQualifiedName());
                properties.put("fullQualifiedResourceName", validBean.getFullQualifiedResourceName());
                final FileInstruction instruction = createFileInstruction();
                // execute instruction:
                final InstructionSet mySet = new PluginInstructionSet();
                mySet.addInstruction(instruction);
                context.addPlaceholderData(properties);
                executor.execute(mySet, context);
            }
        }

        message.setValue("Please rebuild and restart your application");
        return message;
    }

    public FileInstruction createFileInstruction() {
        final FileInstruction instruction = new FileInstruction();
        getInjector().autowireBean(instruction);
        instruction.setAction("copy");
        instruction.setOverwrite(false);
        instruction.setBinary(false);
        instruction.setSource("BeanNameResource.txt");
        instruction.setTarget("{{restFolder}}/{{beanName}}Resource.java");
        return instruction;
    }

    private Set<ValidBean> annotateBeans(final String input, final PluginContext context) {
        final Set<ValidBean> validBeans = new HashSet<>();

        if (Strings.isNullOrEmpty(input)) {
            log.error("No beans were selected");
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
                AnnotationUtils.addKnownAdapters(source);
                final ValidBean bean = new ValidBean();
                bean.setBeanName(className);
                bean.setBeanPath(path);
                bean.setBeanPackage(JavaSourceUtils.getPackageName(filePath));
                final String qualifiedClassName = JavaSourceUtils.getFullQualifiedClassName(filePath);
                bean.setFullQualifiedName(qualifiedClassName);
                final String resourceName = MessageFormat.format("{0}.{1}Resource", context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_REST_PACKAGE), className);
                bean.setFullQualifiedResourceName(resourceName);
                validBeans.add(bean);
            }
        }
        return validBeans;
    }


}

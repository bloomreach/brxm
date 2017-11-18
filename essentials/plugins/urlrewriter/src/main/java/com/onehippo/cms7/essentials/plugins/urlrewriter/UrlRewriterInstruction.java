/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package com.onehippo.cms7.essentials.plugins.urlrewriter;

import java.io.IOException;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.utils.WebXmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modify the site's web.xml to install the rewrite filter.
 */
public class UrlRewriterInstruction implements Instruction {

    private static final Logger logger = LoggerFactory.getLogger(UrlRewriterInstruction.class);
    private static final String BEANS_MAPPING = "classpath*:org/onehippo/forge/**/*.class";
    private static final String FILTER_CLASS = "org.onehippo.forge.rewriting.HippoRewriteFilter";
    private static final String FILTER_NAME = "RewriteFilter";

    @Override
    public String getMessage() {
        return "Install URL Rewriter filter into Site web.xml";
    }

    @Override
    public void setMessage(String message) { }

    @Override
    public InstructionStatus process(PluginContext context) {
        if (!WebXmlUtils.addHstBeanMapping(context, BEANS_MAPPING)) {
            return InstructionStatus.FAILED;
        }

        boolean effect = false;
        try {
            final String filter = readResource("instructions/xml/webxml/rewrite-filter.xml");
            if (WebXmlUtils.addFilter(context, TargetPom.SITE, FILTER_CLASS, filter)) {
                effect = true;
            }
            final String filterMapping = readResource("instructions/xml/webxml/rewrite-filter-mapping.xml");
            if (WebXmlUtils.insertFilterMapping(context, TargetPom.SITE, FILTER_NAME, filterMapping, "XSSUrlFilter")) {
                effect = true;
            }
            if (WebXmlUtils.addDispatcherToFilterMapping(context, TargetPom.SITE, "HstFilter", WebXmlUtils.Dispatcher.REQUEST)) {
                effect = true;
            }
            if (WebXmlUtils.addDispatcherToFilterMapping(context, TargetPom.SITE, "HstFilter", WebXmlUtils.Dispatcher.FORWARD)) {
                effect = true;
            }
        } catch (IOException | JAXBException e) {
            logger.error("Error executing robots plugin instruction", e);
            return InstructionStatus.FAILED;
        }
        return effect ? InstructionStatus.SUCCESS : InstructionStatus.SKIPPED;
    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) { }

    private String readResource(final String resourcePath) throws IOException {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourcePath));
    }
}

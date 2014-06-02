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

package org.onehippo.cms7.essentials.plugins.robots;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.generated.jaxb.WebXml;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.model.DependencyType;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class RobotsInstruction implements Instruction {

    private static Logger log = LoggerFactory.getLogger(RobotsInstruction.class);
    private static final String BEANS_MAPPINGS = "classpath*:org/onehippo/cms7/hst/beans/**/*.class,classpath*:org/onehippo/forge/robotstxt/**/*.class";

    @Override
    public String getMessage() {
        return "Adding bean mappings to web.xml: " + BEANS_MAPPINGS;
    }

    @Override
    public void setMessage(final String message) {

    }

    @Override
    public String getAction() {
        return null;
    }

    @Override
    public void setAction(final String action) {

    }

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        final String webXmlPath = ProjectUtils.getWebXmlPath(DependencyType.SITE);
        final WebXml webXml = ProjectUtils.readWebXmlFile(webXmlPath);
        try {
            final String newContent = webXml.addToHstBeanContextValue(new FileInputStream(webXmlPath), BEANS_MAPPINGS);
            GlobalUtils.writeToFile(newContent, new File(webXmlPath).toPath());
            log.debug("Added new content to {}", webXmlPath);
        } catch (FileNotFoundException e) {
            log.error("Error executing robots plugin instruction", e);
            return InstructionStatus.FAILED;
        }

        return InstructionStatus.SUCCESS;
    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) {
        // noop
    }
}

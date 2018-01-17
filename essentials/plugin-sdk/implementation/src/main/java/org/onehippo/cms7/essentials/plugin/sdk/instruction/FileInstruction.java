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

package org.onehippo.cms7.essentials.plugin.sdk.instruction;

import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.essentials.sdk.api.ctx.PluginContext;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement(name = "file", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class FileInstruction extends BuiltinInstruction {

    public enum Action {
        COPY, DELETE
    }

    private static final Logger log = LoggerFactory.getLogger(FileInstruction.class);

    @Inject private ProjectService projectService;

    private boolean binary;
    private boolean overwrite;
    private String source;
    private String target;
    private Action action = Action.COPY;

    public FileInstruction() {
        super(Type.FILE_CREATE);
    }

    @Override
    public Status execute(final PluginContext context) {
        switch (action) {
            case COPY:
                if (StringUtils.isBlank(source) || StringUtils.isBlank(target)) {
                    log.error("Invalid file instruction '{}'.", toString());
                    return Status.FAILED;
                }
                return projectService.copyResource("/" + source, target, context, overwrite, binary)
                        ? Status.SUCCESS : Status.FAILED;
            case DELETE:
                if (StringUtils.isBlank(target)) {
                    log.error("Invalid file instruction '{}'.", toString());
                    return Status.FAILED;
                }
                return projectService.deleteFile(target, context) ? Status.SUCCESS : Status.FAILED;
        }

        log.error("Unsupported file instruction action '{}'.", action);
        return Status.FAILED;
    }

    @Override
    void populateDefaultChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        if (action == Action.COPY) {
            changeMessageQueue.accept(Type.FILE_CREATE, "Create project file '" + target + "'.");
        } else {
            changeMessageQueue.accept(Type.FILE_DELETE, "Delete project file '" + target + "'.");
        }
    }

    @XmlAttribute
    public boolean isBinary() {
        return binary;
    }

    public void setBinary(final boolean binary) {
        this.binary = binary;
    }

    @XmlAttribute
    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(final boolean overwrite) {
        this.overwrite = overwrite;
    }

    @XmlAttribute
    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    @XmlAttribute
    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    @XmlAttribute
    public String getAction() {
        return action != null ? action.toString().toLowerCase() : null;
    }

    public void setAction(final String action) {
        this.action = Action.valueOf(action.toUpperCase());
    }

    @XmlTransient
    public Action getActionEnum() {
        return action;
    }

    public void setActionEnum(final Action action) {
        this.action = action;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FileInstruction{");
        sb.append(", overwrite=").append(overwrite);
        sb.append(", source='").append(source).append('\'');
        sb.append(", target='").append(target).append('\'');
        sb.append(", action='").append(action).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

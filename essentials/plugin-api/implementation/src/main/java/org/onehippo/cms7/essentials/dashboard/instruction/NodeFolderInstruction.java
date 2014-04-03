/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.instruction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;


/**
 * @version "$Id$"
 */
@Component
@XmlRootElement(name = "folder", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class NodeFolderInstruction extends PluginInstruction {

    private static Logger log = LoggerFactory.getLogger(NodeFolderInstruction.class);
    private String message;
    private String template;
    private String path;
    private PluginContext context;

    @Value("${instruction.message.folder.create}")
    private String messageSuccess;

    // path="/foo/bar/foobar" template="/my_folder_template.xml"

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        this.context = context;
        if (Strings.isNullOrEmpty(path) || Strings.isNullOrEmpty(template)) {
            log.error("Invalid instruction:", this);
            return InstructionStatus.FAILED;
        }
        final Map<String, Object> data = context.getPlaceholderData();
        super.processPlaceholders(data);
        final String myPath = TemplateUtils.replaceTemplateData(path, data);
        if (myPath != null) {
            path = myPath;
        }
        return createFolders();

    }

    private InstructionStatus createFolders() {

        final Session session = context.createSession();

        InputStream stream = null;
        try {
            if (session.itemExists(path)) {
                return InstructionStatus.SKIPPED;
            }
            stream = getClass().getClassLoader().getResourceAsStream(template);
            if (stream == null) {
                log.error("Template was not found: {}", template);
                return InstructionStatus.FAILED;
            }
            String content = GlobalUtils.readStreamAsText(stream);
            final Map<String, Object> data = context.getPlaceholderData();
            final Iterable<String> pathParts = Splitter.on('/').omitEmptyStrings().split(path);
            Node parent = session.getRootNode();
            for (String part : pathParts) {
                if (parent.hasNode(part)) {
                    parent = parent.getNode(part);
                    continue;
                }
                // replace node name

                data.put("name", part);
                final String folderXml = TemplateUtils.replaceTemplateData(content, data);
                session.importXML(parent.getPath(), new ByteArrayInputStream(folderXml.getBytes()), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                parent = parent.getNode(part);

            }
            data.put("target", path);
            message = TemplateUtils.replaceTemplateData(messageSuccess, data);
            session.save();
            sendEvents();
            return InstructionStatus.SUCCESS;


        } catch (RepositoryException | IOException e) {
            log.error("Error creating folders" + this, e);
            GlobalUtils.refreshSession(session, false);
        } finally {
            IOUtils.closeQuietly(stream);
            GlobalUtils.cleanupSession(session);
        }
        return InstructionStatus.FAILED;
    }

    @XmlAttribute
    public String getTemplate() {
        return template;
    }

    public void setTemplate(final String template) {
        this.template = template;
    }

    @XmlAttribute
    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    @Override
    @XmlAttribute
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(final String message) {
        this.message = message;
    }

    @Override
    public String getAction() {
        return "folder";
    }

    @Override
    public void setAction(final String action) {
        // ignore
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NodeFolderInstruction{");
        sb.append("message='").append(message).append('\'');
        sb.append(", template='").append(template).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

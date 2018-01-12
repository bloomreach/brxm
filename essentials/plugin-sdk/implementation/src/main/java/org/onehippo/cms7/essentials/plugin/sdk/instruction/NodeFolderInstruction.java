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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.service.JcrService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * @version "$Id$"
 */
@Component
@XmlRootElement(name = "folder", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class NodeFolderInstruction extends BuiltinInstruction {

    private static Logger log = LoggerFactory.getLogger(NodeFolderInstruction.class);
    private String template;
    private String path;

    @Inject
    JcrService jcrService;

    public NodeFolderInstruction() {
        super(Type.XML_NODE_FOLDER_CREATE);
    }

    @Override
    public Status execute(final PluginContext context) {
        if (Strings.isNullOrEmpty(path) || Strings.isNullOrEmpty(template)) {
            log.error("Invalid instruction:", this);
            return Status.FAILED;
        }
        final Map<String, Object> data = context.getPlaceholderData();
        final String myPath = TemplateUtils.replaceTemplateData(path, data);
        if (myPath != null) {
            path = myPath;
        }
        return createFolders(context);
    }

    @Override
    void populateDefaultChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(getDefaultGroup(), "Create repository folder '" + path + "'.");
    }

    private Status createFolders(final PluginContext context) {
        final Session session = jcrService.createSession();
        InputStream stream = null;
        try {
            if (session.itemExists(path)) {
                return Status.SKIPPED;
            }
            stream = getClass().getClassLoader().getResourceAsStream(template);
            if (stream == null) {
                log.error("Template was not found: {}", template);
                return Status.FAILED;
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
            session.save();
            log.info("Created '{}' from '{}'.", path, template);
            return Status.SUCCESS;
        } catch (RepositoryException | IOException e) {
            log.error("Error creating folders" + this, e);
        } finally {
            IOUtils.closeQuietly(stream);
            jcrService.destroySession(session);
        }
        return Status.FAILED;
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
    public String toString() {
        final StringBuilder sb = new StringBuilder("NodeFolderInstruction{");
        sb.append(", template='").append(template).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

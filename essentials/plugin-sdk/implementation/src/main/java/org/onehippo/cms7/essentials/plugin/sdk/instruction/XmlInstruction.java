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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.Strings;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.TemplateUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.XmlUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.xml.XmlNode;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.PlaceholderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@XmlRootElement(name = "xml", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class XmlInstruction extends BuiltinInstruction {

    public enum Action {
        COPY, DELETE
    }

    @Inject private JcrService jcrService;
    @Inject private PlaceholderService placeholderService;

    private static final Logger log = LoggerFactory.getLogger(XmlInstruction.class);
    private boolean overwrite;
    private String source;
    private String target;
    private Action action;

    public XmlInstruction() {
        super(Type.XML_NODE_CREATE);
    }

    @Override
    public Status execute(final Map<String, Object> parameters) {
        final Map<String, Object> placeholderData = placeholderService.makePlaceholders();
        processPlaceholders(placeholderData);
        log.debug("executing XML Instruction {}", this);
        if (!valid()) {
            log.info("Invalid instruction descriptor: {}", toString());
            return Status.FAILED;
        }
        if (action == Action.COPY) {
            return copy(placeholderData);
        } else {
            return delete();
        }
    }

    @Override
    void populateDefaultChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        if (action == Action.COPY) {
            changeMessageQueue.accept(Type.XML_NODE_CREATE, "Import file '" + source + "' to repository parent node '" + target + "'.");
        } else {
            changeMessageQueue.accept(Type.XML_NODE_DELETE, "Delete repository node '" + target + "'.");
        }
    }

    private Status copy(final Map<String, Object> placeholderData) {
        final Session session = jcrService.createSession();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(source);
        try {
            if (!session.itemExists(target)) {
                log.error("Target node doesn't exist {}", target);
                return Status.FAILED;
            }
            final Node destination = session.getNode(target);

            if (stream == null) {
                log.error("Source file not found {}", source);
                return Status.FAILED;
            }

            // first check if node exists:
            if (!isOverwrite() && nodeExists(placeholderData, session, source, destination.getPath())) {
                log.info("Skipping XML import, target node '{}' already exists.", target);
                return Status.SKIPPED;
            }

            // Import XML with replaced NAMESPACE placeholder
            final String myData = TemplateUtils.replaceTemplateData(GlobalUtils.readStreamAsText(stream), placeholderData);
            session.importXML(destination.getPath(), IOUtils.toInputStream(myData, StandardCharsets.UTF_8), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
            session.save();
            log.info("Imported XML from '{}' to '{}'.", source, target);
        } catch (RepositoryException | IOException e) {
            log.error("Error on copy node", e);
            return Status.FAILED;
        } finally {
            IOUtils.closeQuietly(stream);
            jcrService.destroySession(session);
        }
        return Status.SUCCESS;
    }

    private boolean nodeExists(final Map<String, Object> placeholderData, final Session session, final String source, final String parentPath) throws RepositoryException {

        final InputStream stream = getClass().getClassLoader().getResourceAsStream(source);
        try {
            final XmlNode xmlNode = XmlUtils.parseXml(stream);
            final String name = TemplateUtils.replaceTemplateData(xmlNode.getName(), placeholderData);
            if (!Strings.isNullOrEmpty(name) && session.itemExists(parentPath)) {
                final String absPath = parentPath.endsWith("/") ? parentPath + name : parentPath + '/' + name;
                if (session.itemExists(absPath)) {
                    log.debug("Node already exists: {}", absPath);
                    return true;
                }
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }

        return false;
    }


    private Status delete() {
        final Session session = jcrService.createSession();
        try {
            if (!session.itemExists(target)) {
                log.error("Target node doesn't exist: {}", target);
                return Status.FAILED;
            }
            session.getNode(target).remove();
            session.save();
            log.info("Deleted node '{}'.", target);
        } catch (RepositoryException e) {
            log.error("Error deleting node", e);
            return Status.FAILED;
        } finally {
            jcrService.destroySession(session);
        }
        return Status.SUCCESS;
    }

    private void processPlaceholders(final Map<String, Object> data) {
        final String myTarget = TemplateUtils.replaceTemplateData(target, data);
        if (myTarget != null) {
            target = myTarget;
        }
        //
        final String mySource = TemplateUtils.replaceTemplateData(source, data);
        if (mySource != null) {
            source = mySource;
        }
    }

    private boolean valid() {
        if (action == null || Strings.isNullOrEmpty(target)) {
            return false;
        }

        if (action == Action.COPY && Strings.isNullOrEmpty(source)) {
            return false;
        }
        return true;
    }

    @XmlAttribute
    public boolean isOverwrite() {
        return overwrite;
    }

    /**
     * Setting overwrite=true is not yet supported! Invoking this method with value parameter override=true will result
     * in an UnsupportedOperationException being thrown
     *
     * @param overwrite
     * @throws UnsupportedOperationException when invoked with overwrite=true
     */
    public void setOverwrite(final boolean overwrite) throws UnsupportedOperationException {
        if (overwrite) {
            throw new UnsupportedOperationException("Setting override=true for XmlInstruction is currently not supported");
        }
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("XmlInstruction{");
        sb.append("source='").append(source).append("', ");
        sb.append("target='").append(target).append("', ");
        sb.append("action='").append(action).append("', ");
        sb.append("overwrite=").append(overwrite);
        sb.append('}');
        return sb.toString();
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
}

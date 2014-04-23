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

package org.onehippo.cms7.essentials.dashboard.instruction;

impo

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.InstructionEvent;
import org.onehippo.cms7.essentials.dashboard.event.MessageEvent;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus; * @version "$Id$"
 */
@Component
@XmlRootElement(name = "file", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class FileInstruction extends PluginInstruction {


    public static final Set<String> VALID_ACTIONS = new ImmutableSet.Builder<String>()
            .add(COPY)
            .add(DELETE)
            .build();
    private static final Logger log = LoggerFactory.getLogger(FileInstruction.class);
    private String message;
    private boolean binary;

    @Inject
    private EventBus eventBus;

    @Value("${instruction.message.file.delete}")
    private String messageDelete;

    @Value("${instruction.message.file.copy}")
    private String messageCopy;

    @Value("${instruction.message.file.copy.error}")
    private String messageCopyError;

    @Value("${instruction.message.folder.create}")
    private String messageFolderCreate;
    private boolean overwrite;
    private String source;
    private String target;
    private String action;
    private String folderMessage;
    private String createdFolders;
    private String createdFoldersTarget;
    private PluginContext context;

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        log.debug("executing FILE Instruction {}", this);
        processPlaceholders(context.getPlaceholderData());
        this.context = context;
        if (!valid()) {
            eventBus.post(new MessageEvent("Invalid instruction descriptor: " + toString()));
            eventBus.post(new InstructionEvent(this));
            return InstructionStatus.FAILED;
        }

        // check action:
        if (action.equals(COPY)) {
            return copy();
        } else if (action.equals(DELETE)) {
            return delete();
        }

        eventBus.post(new InstructionEvent(this));
        return InstructionStatus.FAILED;
    }

    private InstructionStatus copy() {
        final File destination = new File(target);
        if (!overwrite && destination.exists()) {
            log.info("File already exists {}", destination);
            eventBus.post(new InstructionEvent(this));
            return InstructionStatus.SKIPPED;
        }

        InputStream stream = null;

        try {
            stream = extractStream();
            if (stream == null) {
                log.error("Stream was null for source: {}", source);
                return InstructionStatus.FAILED;
            }
            if (destination.exists()) {
                final boolean success = destination.delete();
                if (!success) {
                    log.error("Failed to delete destination file: {}", destination);
                    return InstructionStatus.FAILED;
                }
            }
            // try to read as resource:
            if (!destination.exists()) {
                createParentDirectories(destination);
            }
            // replace file placeholders if needed:
            if (isBinary()) {
                FileUtils.copyInputStreamToFile(stream, destination);
            } else {
                final String content = GlobalUtils.readStreamAsText(stream);
                final String replacedData = TemplateUtils.replaceTemplateData(content, context.getPlaceholderData());
                FileUtils.copyInputStreamToFile(IOUtils.toInputStream(replacedData), destination);
            }
            sendEvents();
            return InstructionStatus.SUCCESS;
        } catch (IOException e) {
            message = messageCopyError;
            log.error("Error while copy resource", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
        eventBus.post(new InstructionEvent(this));
        return InstructionStatus.FAILED;
    }

    protected InputStream extractStream() throws FileNotFoundException {
        // try to read file first:
        final String encoded  = GlobalUtils.decodeUrl(source);
        final File file = new File(encoded );
        if (file.exists()) {
            return new FileInputStream(file);
        } else {
            return getClass().getClassLoader().getResourceAsStream(encoded );
        }
    }

    /**
     * Recursively creates parent directories in case they don't exist yet
     *
     * @param destination starting directory
     * @throws IOException
     */
    protected void createParentDirectories(final File destination) throws IOException {

        Deque<String> directories = new ArrayDeque<>();
        String parent = destination.getParent();
        while (!new File(parent).exists()) {
            directories.push(parent);
            parent = new File(parent).getParent();
        }
        processDirectories(directories);

        Files.createFile(destination.toPath());
    }

    private void processDirectories(final Deque<String> directories) throws IOException {
        if (!directories.isEmpty()) {
            folderMessage = directories.size() > 1 ? directories.size() - 1 + " directories" : "directory";
            createdFolders = directories.getLast().substring(directories.getFirst().length());
            createdFoldersTarget = directories.getLast();
            Files.createDirectories(new File(directories.getLast()).toPath());
            eventBus.post(new InstructionEvent(messageFolderCreate));
        }
    }

    @XmlAttribute(name = "binary")
    public boolean isBinary() {
        return binary;
    }

    public void setBinary(final boolean binary) {
        this.binary = binary;
    }

    private InstructionStatus delete() {
        try {
            Path path = new File(target).toPath();
            final boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                sendEvents();
                log.debug("Deleted file {}", target);
                return InstructionStatus.SUCCESS;
            } else {
                log.debug("File not deleted {}", target);
                eventBus.post(new InstructionEvent(this));
                return InstructionStatus.SKIPPED;
            }
        } catch (IOException e) {
            log.error("Error deleting file", e);
        }
        eventBus.post(new InstructionEvent(this));
        return InstructionStatus.FAILED;
    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) {
        final String myTarget = TemplateUtils.replaceTemplateData(target, data);
        if (myTarget != null) {
            target = myTarget;
        }
        //
        final String mySource = TemplateUtils.replaceTemplateData(source, data);
        if (mySource != null) {
            source = mySource;
        }
        // add local data
        data.put(EssentialConst.PLACEHOLDER_SOURCE, source);
        data.put(EssentialConst.PLACEHOLDER_TARGET, target);
        //TODO check what Wicket can offer regarding placeholders and localization, it's probably reusable
        data.put("folderMessage", folderMessage);
        data.put("createdFolders", createdFolders);
        data.put("createdFoldersTarget", createdFoldersTarget);
        // setup messages:

        if (Strings.isNullOrEmpty(message) && !Strings.isNullOrEmpty(action)) {
            // check message based on action:
            if (action.equals(COPY)) {
                message = messageCopy;
            } else if (action.equals(DELETE)) {
                message = messageDelete;
            }
        }

        super.processPlaceholders(data);
        //
        messageCopyError = TemplateUtils.replaceTemplateData(messageCopyError, data);
        message = TemplateUtils.replaceTemplateData(message, data);
    }

    protected boolean valid() {
        if (Strings.isNullOrEmpty(action) || !VALID_ACTIONS.contains(action) || Strings.isNullOrEmpty(target)) {
            return false;
        }
        if (action.equals(COPY) && (Strings.isNullOrEmpty(source))) {
            return false;
        }
        return true;
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
    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(final String message) {
        this.message = message;
    }

    @XmlAttribute
    @Override
    public String getAction() {
        return action;
    }

    @Override
    public void setAction(final String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FileInstruction{");
        sb.append("message='").append(message).append('\'');
        sb.append(", overwrite=").append(overwrite);
        sb.append(", source='").append(source).append('\'');
        sb.append(", target='").append(target).append('\'');
        sb.append(", action='").append(action).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialsFileUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DirectoryInstruction allows you to create an empty directory in your project, or to copy an entire directory
 * into your project.
 */
@Component
@XmlRootElement(name = "directory", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class DirectoryInstruction extends BuiltinInstruction {

    public enum Action {
        CREATE, COPY
    }

    private static final Logger log = LoggerFactory.getLogger(DirectoryInstruction.class);
    private String target;
    private Action action;
    private String source;
    private boolean overwrite;

    /**
     * Files with those extensions will be template-processed (replacing of template placeholders)
     */
    private static final Set<String> SUPPORTED_PLACEHOLDER_EXTENSIONS = new ImmutableSet.Builder<String>()
            .add("properties")
            .add("jspf")
            .add("md")
            .add("groovy")
            .add("cnd")
            .add("log")
            .add("json")
            .add("java")
            .add("jsp")
            .add("xml")
            .add("html")
            .add("htm")
            .add("txt")
            .add("ftl")
            .add("css")
            .add("js")
            .build();

    public DirectoryInstruction() {
        super(Type.FILE_CREATE);
    }

    @Override
    public Status execute(final PluginContext context) {
        if (action == null) {
            log.warn("DirectoryInstruction: action was empty");
            return Status.FAILED;
        }
        if (Strings.isNullOrEmpty(target)) {
            log.warn("DirectoryInstruction: target was empty");
            return Status.FAILED;
        }
        processPlaceholders(context.getPlaceholderData());
        switch (action) {
            case CREATE:
                return create();
            case COPY:
                return copy(context.getPlaceholderData());
        }
        return Status.FAILED;
    }

    @Override
    void populateDefaultChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        final String message = action == DirectoryInstruction.Action.COPY
                ? "Copy directory '" + source + "' to project directory '" + target + "'."
                : "Create directory '" + target + "'.";
        changeMessageQueue.accept(getDefaultGroup(), message);
    }


    private Status copy(final Map<String, Object> placeholderData) {
        if (Strings.isNullOrEmpty(source)) {
            log.warn("Source was not defined");
            return Status.FAILED;
        }
        if (Strings.isNullOrEmpty(target)) {
            log.warn("target was not defined");
            return Status.FAILED;
        }

        return copyResources(source, new File(target), placeholderData);
    }

    private Status create() {
        log.debug("Creating directory: {}", target);
        try {
            EssentialsFileUtils.createParentDirectories(new File(target));
        } catch (IOException e) {
            log.error("Error creating directory: " + target, e);
            return Status.FAILED;
        }
        return Status.SUCCESS;
    }

    private void processPlaceholders(final Map<String, Object> data) {
        final String myTarget = TemplateUtils.replaceTemplateData(target, data);
        if (myTarget != null) {
            target = myTarget;
        }
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

    @XmlAttribute
    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }


    @XmlAttribute
    public boolean isOverwrite() {
        return overwrite;
    }


    public void setOverwrite(final boolean overwrite) {
        this.overwrite = overwrite;
    }
    //############################################
    //
    //############################################

    private Status copyResources(final String source, final File targetDirectory, final Map<String, Object> placeholderData) {

        if (!targetDirectory.exists()) {
            log.warn("Directory {} doesn't exist, creating new one", targetDirectory);
            try {
                EssentialsFileUtils.createParentDirectories(targetDirectory);
            } catch (IOException e) {
                log.error("Error creating directory", e);
                return Status.FAILED;
            }

        }

        try {
            final JarURLConnection connection = createConnection(source);
            if (connection == null) {
                log.warn("Couldn't process jar source: {}", source);
                return Status.FAILED;
            }
            final String name = connection.getEntryName();
            log.debug("Processing zip file for connection: {}", name);
            final JarFile jarFile = connection.getJarFile();
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry jarEntry = entries.nextElement();
                final String entryName = jarEntry.getName();
                log.debug("Processing jar entry: {}", entryName);
                if (entryName.startsWith(name + '/')) {
                    final String fileName = entryName.substring(name.length());
                    final File file = new File(targetDirectory, fileName);
                    if (file.exists()) {
                        if (overwrite) {
                            final boolean delete = file.delete();
                            log.info("Deleted existing file: {},{}", file, delete);
                        } else {
                            log.info("File already exists, skipping copy: {}", file);
                            continue;
                        }
                    }
                    if (jarEntry.isDirectory()) {
                        final boolean created = file.mkdirs();
                        log.debug("Created directory:{},  {}", file, created);
                    } else {
                        final InputStream is = jarFile.getInputStream(jarEntry);
                        final OutputStream out = org.apache.commons.io.FileUtils.openOutputStream(file);
                        IOUtils.copy(is, out);
                        IOUtils.closeQuietly(is);
                        IOUtils.closeQuietly(out);
                        log.info("Copied file {}", file);
                        final String ext = FilenameUtils.getExtension(file.getAbsolutePath());
                        if (SUPPORTED_PLACEHOLDER_EXTENSIONS.contains(ext.toLowerCase())) {
                            TemplateUtils.replaceFileTemplateData(file.toPath(), placeholderData);
                        } else {
                            log.debug("Skipping processing of template placeholders: {}", ext);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error extracting files", e);
            return Status.FAILED;
        }

        return Status.SUCCESS;
    }

    private JarURLConnection createConnection(final String source) throws IOException {
        if (Strings.isNullOrEmpty(source)) {
            return null;
        }
        final URL resource = getClass().getResource(source);
        if (resource == null) {
            return null;
        }
        log.info("Processing resource {}", resource);
        final URLConnection urlConnection = resource.openConnection();
        if (urlConnection instanceof JarURLConnection) {
            return (JarURLConnection)urlConnection;
        }
        return null;
    }
}

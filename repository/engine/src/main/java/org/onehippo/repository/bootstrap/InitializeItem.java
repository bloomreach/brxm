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
package org.onehippo.repository.bootstrap;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.LocalHippoRepository;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.bootstrap.instructions.ContentDeleteInstruction;
import org.onehippo.repository.bootstrap.instructions.ContentFromNodeInstruction;
import org.onehippo.repository.bootstrap.instructions.ContentPropAddInstruction;
import org.onehippo.repository.bootstrap.instructions.ContentPropDeleteInstruction;
import org.onehippo.repository.bootstrap.instructions.ContentPropSetInstruction;
import org.onehippo.repository.bootstrap.instructions.ContentResourceInstruction;
import org.onehippo.repository.bootstrap.instructions.NamespaceInstruction;
import org.onehippo.repository.bootstrap.instructions.NodeTypesInstruction;
import org.onehippo.repository.bootstrap.instructions.NodeTypesResourceInstruction;
import org.onehippo.repository.bootstrap.instructions.WebResourceBundleInstruction;

import static org.apache.commons.lang.StringUtils.trim;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_DELTADIRECTIVE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTDELETE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPADD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPDELETE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPSET;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTROOT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTEXTPATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_EXTENSIONSOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAMESPACE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NODETYPES;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NODETYPESRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELOADONSTARTUP;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_STATUS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_UPSTREAMITEMS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_WEBRESOURCEBUNDLE;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.ITEM_STATUS_DONE;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.ITEM_STATUS_FAILED;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

public class InitializeItem {

    private static final String[] INSTRUCTION_PROPERTIES = {
            HIPPO_NAMESPACE,
            HIPPO_NODETYPESRESOURCE,
            HIPPO_NODETYPES,
            HIPPO_CONTENTDELETE,
            HIPPO_CONTENTPROPDELETE,
            HIPPO_CONTENTRESOURCE,
            HIPPO_CONTENT,
            HIPPO_CONTENTPROPSET,
            HIPPO_CONTENTPROPADD,
            HIPPO_WEBRESOURCEBUNDLE
    };

    private Node itemNode;
    private Node tempItemNode;
    private Node initFolder;
    private List<InitializeInstruction> instructions;

    public InitializeItem(final Node itemNode) {
        this.itemNode = itemNode;
    }

    public InitializeItem(final Node tempItemNode, final Node initFolder) {
        this.tempItemNode = tempItemNode;
        this.initFolder = initFolder;
    }

    public void validate() throws RepositoryException {
        List<InitializeInstruction> instructions = getInstructions();
        if (instructions.isEmpty()) {
            throw new RepositoryException("No instructions");
        }
        if (instructions.size() > 2) {
            throw new RepositoryException("Instructions cannot be combined");
        }
        if (instructions.size() == 2) {
            if (!instructions.get(0).canCombine(instructions.get(1))) {
                if (instructions.get(1).canCombine(instructions.get(0))) {
                    throw new IllegalStateException();
                }
                throw new RepositoryException(String.format("%s cannot be combined with %s",
                        instructions.get(0).getName(), instructions.get(1).getName()));
            }
        }
    }

    public String getName() throws RepositoryException {
        return itemNode.getName();
    }

    public String getContentResource() throws RepositoryException {
        return trim(JcrUtils.getStringProperty(itemNode, HIPPO_CONTENTRESOURCE, null));
    }

    public URL getContentResourceURL() throws RepositoryException {
        final String contentResource = getContentResource();
        return contentResource != null ? getResourceURL(contentResource) : null;

    }

    public InputStream getContent() throws RepositoryException {
        return itemNode.getProperty(HIPPO_CONTENT).getBinary().getStream();
    }

    private URL getResourceURL(final String contentResource) throws RepositoryException {
        try {
            if (contentResource.startsWith("file:")) {
                return URI.create(contentResource).toURL();
            } else {
                final String extension = getExtensionSource();
                if (extension != null) {
                    URL resource = new URL(extension);
                    resource = new URL(resource, contentResource);
                    return resource;
                } else {
                    return LocalHippoRepository.class.getResource(contentResource);
                }
            }
        } catch (MalformedURLException e) {
            throw new RepositoryException("Failed to create content resource URL", e);
        }
    }

    public List<InitializeInstruction> getInstructions() throws RepositoryException {
        if (instructions == null) {
            instructions = createInstructions();
        }
        return instructions;
    }

    public String getContentRoot() throws RepositoryException {
        return trim(JcrUtils.getStringProperty(itemNode, HIPPO_CONTENTROOT, "/"));
    }

    public String getNodetypesResource() throws RepositoryException {
        return trim(JcrUtils.getStringProperty(itemNode, HIPPO_NODETYPESRESOURCE, null));
    }

    public URL getNodetypesResourceURL() throws RepositoryException {
        final String nodetypesResource = getNodetypesResource();
        return nodetypesResource != null ? getResourceURL(nodetypesResource) : null;
    }

    public InputStream getNodetypes() throws RepositoryException {
        return itemNode.getProperty(HIPPO_NODETYPES).getBinary().getStream();
    }

    public String getExtensionSource() throws RepositoryException {
        return trim(JcrUtils.getStringProperty(itemNode, HIPPO_EXTENSIONSOURCE, null));
    }

    public String getContextPath() throws RepositoryException {
        final String[] contextPaths = getContextPaths();
        if (contextPaths != null && contextPaths.length > 0) {
            return contextPaths[0];
        }
        return null;
    }

    private String[] getContextPaths() throws RepositoryException {
        return JcrUtils.getMultipleStringProperty(itemNode, HIPPO_CONTEXTPATHS, null);
    }

    public boolean isReloadable() throws RepositoryException {
        if (JcrUtils.getBooleanProperty(itemNode, HIPPO_RELOADONSTARTUP, false)) {
            final String deltaDirective = trim(JcrUtils.getStringProperty(itemNode, HIPPOSYS_DELTADIRECTIVE, null));
            if (deltaDirective != null && (deltaDirective.equals("combine") || deltaDirective.equals("overlay"))) {
                log.error("Cannot reload initialize item {} because it is a combine or overlay delta", itemNode.getName());
                return false;
            }
            return true;
        }
        return false;
    }

    public Collection<InitializeItem> getUpstreamItems() throws RepositoryException {
        List<InitializeItem> upstreamItems = new ArrayList<>();
        for (String upstreamItemId : getUpstreamItemIds()) {
            Node upstreamItemNode = itemNode.getSession().getNodeByIdentifier(upstreamItemId);
            upstreamItems.add(new InitializeItem(upstreamItemNode));
        }
        return upstreamItems;
    }

    private String[] getUpstreamItemIds() throws RepositoryException {
        return JcrUtils.getMultipleStringProperty(itemNode, HIPPO_UPSTREAMITEMS, new String[]{});
    }

    public boolean isDownstreamItem() throws RepositoryException {
        return getUpstreamItemIds().length > 0;
    }

    public boolean areUpstreamItemsDone() throws RepositoryException {
        boolean done = true;
        for (InitializeItem upstreamItem : getUpstreamItems()) {
            done &= upstreamItem.isDone();
        }
        return done;
    }

    public void setContextPaths(Collection<String> contextPaths) throws RepositoryException {
        itemNode.setProperty(HIPPO_CONTEXTPATHS, contextPaths.toArray(new String[contextPaths.size()]));
    }

    public String getNamespace() throws RepositoryException {
        return trim(itemNode.getProperty(HIPPO_NAMESPACE).getString());
    }

    public String getContentDeletePath() throws RepositoryException {
        return trim(itemNode.getProperty(HIPPO_CONTENTDELETE).getString());
    }

    public String getContentPropDeletePath() throws RepositoryException {
        return trim(itemNode.getProperty(HIPPO_CONTENTPROPDELETE).getString());
    }

    public String getWebResourceBundle() throws RepositoryException {
        return trim(itemNode.getProperty(HIPPO_WEBRESOURCEBUNDLE).getString());
    }

    public Property getContentPropSetProperty() throws RepositoryException {
        return itemNode.getProperty(HIPPO_CONTENTPROPSET);
    }

    public Property getContentPropAddProperty() throws RepositoryException {
        return itemNode.getProperty(HIPPO_CONTENTPROPADD);
    }

    public boolean isDone() throws RepositoryException {
        return ITEM_STATUS_DONE.equals(JcrUtils.getStringProperty(itemNode, HIPPO_STATUS, null));
    }

    private void setStatus(String status) throws RepositoryException {
        itemNode.setProperty(HIPPO_STATUS, status);
    }

    private void clearUpstreamItems() throws RepositoryException {
        if (itemNode.hasProperty(HIPPO_UPSTREAMITEMS)) {
            itemNode.getProperty(HIPPO_UPSTREAMITEMS).remove();
        }
    }

    List<PostStartupTask> process() throws RepositoryException {
        try {
            if (isDownstreamItem() && !areUpstreamItemsDone()) {
                log.debug("Not executing downstream item {}: upstream item unsuccessfully executed", getName());
                return Collections.emptyList();
            }
            final List<PostStartupTask> postStartupTasks = new ArrayList<>();
            for (InitializeInstruction instruction : getInstructions()) {
                log.info("Executing " + instruction);
                final PostStartupTask postStartupTask = instruction.execute();
                if (postStartupTask != null) {
                    postStartupTasks.add(postStartupTask);
                }
            }
            setStatus(ITEM_STATUS_DONE);
            return Collections.unmodifiableList(postStartupTasks);
        } catch (RepositoryException e) {
            setStatus(ITEM_STATUS_FAILED);
            throw e;
        } finally {
            try {
                clearUpstreamItems();
            } catch (RepositoryException e) {
                log.error(e.toString(), e);
            }
        }
    }

    void initialize() throws RepositoryException {
    }

    private List<InitializeInstruction> createInstructions() throws RepositoryException {
        List<InitializeInstruction> instructions = new ArrayList<>();
        for (Property property : getInstructionProperties()) {
            final InitializeInstruction instruction = createInstruction(property);
            if (instruction != null) {
                instructions.add(instruction);
            }
        }
        return instructions;
    }

    private InitializeInstruction createInstruction(Property property) throws RepositoryException {
        switch (property.getName()) {
            case HIPPO_CONTENTRESOURCE : return new ContentResourceInstruction(this, itemNode.getSession());
            case HIPPO_NAMESPACE : return new NamespaceInstruction(this, itemNode.getSession());
            case HIPPO_NODETYPESRESOURCE : return new NodeTypesResourceInstruction(this, itemNode.getSession());
            case HIPPO_NODETYPES : return new NodeTypesInstruction(this, itemNode.getSession());
            case HIPPO_CONTENTDELETE : return new ContentDeleteInstruction(this, itemNode.getSession());
            case HIPPO_CONTENTPROPDELETE : return new ContentPropDeleteInstruction(this, itemNode.getSession());
            case HIPPO_CONTENT : return new ContentFromNodeInstruction(this, itemNode.getSession());
            case HIPPO_CONTENTPROPSET : return new ContentPropSetInstruction(this, itemNode.getSession());
            case HIPPO_CONTENTPROPADD : return new ContentPropAddInstruction(this, itemNode.getSession());
            case HIPPO_WEBRESOURCEBUNDLE : return new WebResourceBundleInstruction(this, itemNode.getSession());
        }
        throw new IllegalStateException("Unknown initialize instruction: " + property.getName());
    }

    private Iterable<Property> getInstructionProperties() throws RepositoryException {
        List<Property> properties = new ArrayList<>();
        for (String instructionProperty : INSTRUCTION_PROPERTIES) {
            if (itemNode.hasProperty(instructionProperty)) {
                properties.add(itemNode.getProperty(instructionProperty));
            }
        }
        return properties;
    }

}

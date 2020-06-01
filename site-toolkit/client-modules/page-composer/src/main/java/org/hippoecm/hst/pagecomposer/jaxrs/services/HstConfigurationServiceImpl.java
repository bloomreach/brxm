/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.HstConfigurationException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_ABSTRACTPAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_COMPONENTS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;

public class HstConfigurationServiceImpl implements HstConfigurationService {
    private static final Logger log = LoggerFactory.getLogger(HstConfigurationServiceImpl.class);

    public static final String PREVIEW_SUFFIX = "-preview";

    private final String configurationsRootPath;

    public HstConfigurationServiceImpl(final String configurationsRootPath) {
        this.configurationsRootPath = configurationsRootPath;
    }

    @Override
    public void delete(final Session session, final String configurationPath) throws RepositoryException, HstConfigurationException {
        final Node configNode = getConfiguration(session, configurationPath);
        deletePreviewConfiguration(session, configurationPath);
        if (hasDescendant(session, configNode.getName())) {
            throw new HstConfigurationException("The configuration node is inherited by others");
        }
        configNode.remove();
    }

    @Override
    public void delete(final HstRequestContext requestContext, final Channel channel) throws RepositoryException, HstConfigurationException {
        try {
            delete(requestContext.getSession(), channel.getHstConfigPath());
            deleteBranches(requestContext, channel);
        } catch (HstConfigurationException e) {
            deleteBranches(requestContext, channel);
            throw e;
        }
    }

    private void deleteBranches(final HstRequestContext requestContext, final Channel master) throws RepositoryException, HstConfigurationException {
        VirtualHost virtualHost = requestContext.getResolvedMount().getMount().getVirtualHost();
        String hostGroupName = virtualHost.getHostGroupName();
        Map<String, Channel> channels = virtualHost.getVirtualHosts().getChannels(hostGroupName);
        List<Channel> branches = channels.values().stream()
                // only the live channels since #delete(session, channel) will also delete the preview
                .filter(channel -> !channel.isPreview() && master.getId().equals(channel.getBranchOf()))
                .collect(Collectors.toList());
        // remove the branches as well
        for (Channel branch : branches) {
            delete(requestContext.getSession(), branch.getHstConfigPath());
        }
    }

    @Override
    public List<Node> getContainerNodes(final Session session, final String configurationPath) throws RepositoryException {
        final Node configurationNode = session.getNode(configurationPath);
        final List<String> childNodeNames = Arrays.asList(NODENAME_HST_PAGES,
                NODETYPE_HST_COMPONENTS,
                NODENAME_HST_ABSTRACTPAGES,
                NODENAME_HST_WORKSPACE);

        final List<Node> containerNodes = new ArrayList<>();
        for (String childNodeName : childNodeNames) {
            if (configurationNode.hasNode(childNodeName)) {
                getContainerNodes(configurationNode.getNode(childNodeName), containerNodes);
            }
        }
        return containerNodes;
    }

    private void getContainerNodes(final Node node, final List<Node> containerNodes) throws RepositoryException {
        if (node.isNodeType(NODETYPE_HST_CONTAINERCOMPONENT)) {
            containerNodes.add(node);
            return;
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            getContainerNodes(child, containerNodes);
        }
    }

    private void deletePreviewConfiguration(final Session session, final String configurationPath) throws RepositoryException {
        final String previewConfigurationPath = configurationPath + PREVIEW_SUFFIX;
        if (session.nodeExists(previewConfigurationPath)) {
            session.removeItem(previewConfigurationPath);
        }
    }




    private Node getConfiguration(final Session session, final String configurationPath) throws RepositoryException, HstConfigurationException {
        validateConfigurationPathArg(configurationPath);

        final Node configurationNode = session.getNode(configurationPath);
        if (!configurationNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION)) {
            throw new HstConfigurationException(String.format("%s has invalid node type: '%s'. It should be '%s'",
                    configurationPath, configurationNode.getPrimaryNodeType().getName(),
                    HstNodeTypes.NODETYPE_HST_CONFIGURATION));
        }
        return configurationNode;
    }

    private NodeIterator getConfigurations(final Session session) throws RepositoryException {
        final Node configurationsRootNode = session.getNode(this.configurationsRootPath);
        return configurationsRootNode.getNodes();
    }

    private boolean hasDescendant(final Session session, final String configId) throws RepositoryException {
        final NodeIterator configNodes = getConfigurations(session);

        while (configNodes.hasNext()) {
            final Node configNode = configNodes.nextNode();
            if (inheritsFrom(configNode, configId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true if the given configuration node inherits from the specified configId
     *
     * @param configNode
     * @param configId
     * @return
     */
    private boolean inheritsFrom(final Node configNode, final String configId) {
        if (StringUtils.isBlank(configId)) {
            throw new IllegalArgumentException("configId must not be blank");
        }

        final String relativeConfigPath = "../" + configId;
        try {
            final String[] inheritsFrom = JcrUtils.getMultipleStringProperty(configNode,
                    HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM, new String[0]);

            return Arrays.stream(inheritsFrom)
                    .filter(relativeConfigPath::equals)
                    .findFirst()
                    .isPresent();
        } catch (RepositoryException e) {
            final String error = String.format("Cannot read '%s' property from '%s",
                    HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM, JcrUtils.getNodePathQuietly(configNode));
            log.warn(error, e);
        }
        return false;
    }

    private void validateConfigurationPathArg(final String configurationPath) {
        if (StringUtils.isBlank(configurationPath)) {
            throw new IllegalArgumentException("configurationPath must not be blank");
        }

        if (!configurationPath.startsWith(configurationsRootPath)) {
            throw new IllegalArgumentException("configurationPath must start with '" + configurationsRootPath + "'");
        }

        if (configurationPath.endsWith(PREVIEW_SUFFIX)) {
            throw new IllegalArgumentException("configurationPath must not end with '" + PREVIEW_SUFFIX + "'");
        }
    }

}

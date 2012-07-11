/*
*  Copyright 2011-2012 Hippo.
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package org.hippoecm.hst.cmsrest.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.rest.DocumentService;
import org.hippoecm.hst.rest.beans.ChannelDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentsResource implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentsResource.class);

    private ChannelManager channelManager;
    private HstLinkCreator hstLinkCreator;

    public void setChannelManager(final ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public void setHstLinkCreator(HstLinkCreator hstLinkCreator) {
        this.hstLinkCreator = hstLinkCreator;
    }

    public List<ChannelDocument> getChannels(String uuid) {
        if (channelManager == null) {
            log.warn("Cannot look up channels for document '{}' because the channel manager is null", uuid);
            return Collections.emptyList();
        }
        if (hstLinkCreator == null) {
            log.warn("Cannot look up channels for document '{}' because hstLinkCreator is null", uuid);
            return Collections.emptyList();
        }

        HstRequestContext requestContext = RequestContextProvider.get();

        Node handle = ResourceUtil.getNode(requestContext, uuid);
        if (handle == null) {
            return Collections.emptyList();
        }
        String hostGroupNameForChannelMngr = requestContext.getResolvedMount().getMount().getVirtualHost().getVirtualHosts().getChannelManagerHostGroupName();
        List<HstLink> canonicalLinks = hstLinkCreator.createAllAvailableCanonicals(handle, requestContext, null, hostGroupNameForChannelMngr);
        List<ChannelDocument> channelDocuments = new ArrayList<ChannelDocument>(canonicalLinks.size());

        for (HstLink link : canonicalLinks) {
            final Mount linkMount = link.getMount();
            final String channelPath = linkMount.getChannelPath();
            if (channelPath == null) {
                log.debug("Skipping link for mount '{}' since it does not have a channel path", linkMount.getName());
                continue;
            }

            try {
                final Channel channel = channelManager.getChannelByJcrPath(channelPath);
                if (channel == null) {
                    log.warn("Skipping link for mount '{}' since its channel path '{}' does not point to a channel",
                            linkMount.getName(), channelPath);

                    continue;
                }

                ChannelDocument document = new ChannelDocument();
                document.setChannelId(channel.getId());
                document.setChannelName(channel.getName());
                if (StringUtils.isNotEmpty(link.getPath())) {
                    document.setPathInfo("/"+link.getPath());
                } else {
                    document.setPathInfo(StringUtils.EMPTY);
                }
                document.setMountPath(link.getMount().getMountPath());
                document.setHostName(link.getMount().getVirtualHost().getHostName());

                // The preview in the cms always accesses the hst site through the hostname of the cms, but
                // adds the contextpath of the website. By default it is site, but if a different contextpath is
                // available for the mount that belongs to the HstLink, we take that one.
                if (link.getMount().onlyForContextPath() != null) {
                    document.setContextPath(link.getMount().onlyForContextPath());
                } else {
                    // if there is no contextpath configured on the Mount belonging to the HstLink, we use the contextpath
                    // from the current HttpServletRequest
                    document.setContextPath(requestContext.getServletRequest().getContextPath());
                }
                
                // and set the contextpath through which the template composer is available
                if(link.getMount().getVirtualHost().getVirtualHosts().getDefaultContextPath() != null) {
                    document.setTemplateComposerContextPath(link.getMount().getVirtualHost().getVirtualHosts().getDefaultContextPath());
                } else {
                    document.setTemplateComposerContextPath(requestContext.getServletRequest().getContextPath());
                }

                // set the cmsPreviewPrefix through which prefix after the contextPath the channels can be accessed
                document.setCmsPreviewPrefix(link.getMount().getVirtualHost().getVirtualHosts().getCmsPreviewPrefix());
                
                channelDocuments.add(document);
            } catch (ChannelException e) {
                log.warn("Error getting channel with path '" + channelPath + "'", e);
            }
        }

        return channelDocuments;
    }

    public String getUrl(String uuid, String type) {
        if (hstLinkCreator == null) {
            log.warn("Cannot generate URL of type '{}' for document with UUID '{}' because hstLinkCreator is null", type, uuid);
            return "";
        }

        HstRequestContext requestContext = RequestContextProvider.get();

        Node handle = ResourceUtil.getNode(requestContext, uuid);
        if (handle == null) {
            return "";
        }

        String hostGroupNameForChannelMngr = requestContext.getResolvedMount().getMount().getVirtualHost().getVirtualHosts().getChannelManagerHostGroupName();
        List<HstLink> canonicalLinks = hstLinkCreator.createAllAvailableCanonicals(handle, requestContext, type, hostGroupNameForChannelMngr);

        if (canonicalLinks.isEmpty()) {
            log.info("Cannot generate URL of type '{}' for document with UUID '{}' because no mount in the host group '{}' matches",
                    new Object[]{type, uuid, hostGroupNameForChannelMngr});

            return "";
        }

        // Determine the 'best' canonical link: the one whose mount has the closest content path
        // {@link Mount#getCanonicalContentPath()} to the path of the document handle. If multiple {@link Mount}'s have
        // an equally well suited {@link Mount#getCanonicalContentPath()}, we pick the mount with the fewest types.
        // These mounts are in general the most generic ones. If multiple {@link Mount}'s have equally well suited
        // {@link Mount#getCanonicalContentPath()} and an equal number of types, we pick one at random.
        List<HstLink> candidateLinks = new ArrayList<HstLink>();
        int bestPathLength = 0;
        for (HstLink link : canonicalLinks) {
            Mount mount = link.getMount();

            if (mount.getCanonicalContentPath().length() == bestPathLength) {
                // equally well as already found ones. Add to the candidates.
                candidateLinks.add(link);
            } else if (mount.getCanonicalContentPath().length() > bestPathLength) {
                // this is a better one than the ones already found. Clear the candidates first.
                candidateLinks.clear();
                candidateLinks.add(link);
                bestPathLength = mount.getCanonicalContentPath().length();
            } else {
                // ignore, we already have a better link
            }
        }

        HstLink bestLink = candidateLinks.get(0);
        int typeCount = Integer.MAX_VALUE;
        for (HstLink link : candidateLinks) {
            final Mount mount = link.getMount();
            if (mount.getTypes().size() < typeCount) {
                typeCount = mount.getTypes().size();
                bestLink = link;
            }
        }

        return bestLink.toUrlForm(requestContext, true);
    }

}

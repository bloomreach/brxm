/*
*  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.core.linking.CompositeHstLinkCreator;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.platform.api.DocumentService;
import org.hippoecm.hst.platform.api.beans.ChannelDocument;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistryImpl;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.PREFER_RENDER_BRANCH_ID;

public class DocumentServiceImpl implements DocumentService  {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private HstModelRegistryImpl hstModelRegistry;
    private final PreviewDecorator previewDecorator;

    public DocumentServiceImpl(final HstModelRegistryImpl hstModelRegistry, final PreviewDecorator previewDecorator) {
        this.hstModelRegistry = hstModelRegistry;
        this.previewDecorator = previewDecorator;
    }

    public List<ChannelDocument> getPreviewChannels(final Session userSession, final String hostGroup, final String uuid) {

        final List<ChannelDocument> channelDocuments = new ArrayList<>();

        Node handle = ResourceUtil.getNode(userSession, uuid);
        if (handle == null) {
            return channelDocuments;
        }

        for (HstModel hstModel : hstModelRegistry.getModels().values()) {

            final Optional<Mount> mountCandidate = hstModel.getVirtualHosts().getMountsByHostGroup(hostGroup).stream().findFirst();
            mountCandidate.ifPresent(mount -> {

                final Mount previewMount = previewDecorator.decorateMountAsPreview(mount);
                HstLinkCreator hstLinkCreator = hstModel.getHstLinkCreator();

                if (hstLinkCreator instanceof CompositeHstLinkCreator) {
                    // local link creator is needed since for every HstModel we request all canonical links AND channelFilter
                    // below can be unique PER webapp
                    hstLinkCreator = ((CompositeHstLinkCreator)hstLinkCreator).getLocalHstLinkCreator();
                }

                List<HstLink> canonicalLinks = hstLinkCreator.createAllAvailableCanonicals(handle, previewMount, null, hostGroup);

                for (HstLink link : canonicalLinks) {
                    final Mount linkMount = link.getMount();
                    final Channel channel = linkMount.getChannel();

                    if (channel == null) {
                        log.debug("Skipping link for mount '{}' since it does not have a channel", linkMount.getName());
                        continue;
                    }

                    final BiPredicate<Session, Channel> channelFilter = ((InternalHstModel) hstModel).getChannelFilter();
                    if (!channelFilter.test(userSession, channel)) {
                        log.info("Skipping channel '{}' because filtered out by channel filters", channel.toString());
                        continue;
                    }

                    ChannelDocument document = new ChannelDocument();
                    document.setChannelId(channel.getId());
                    document.setChannelName(channel.getName());
                    document.setBranchId(channel.getBranchId());
                    document.setBranchOf(channel.getBranchOf());
                    if (StringUtils.isNotEmpty(link.getPath())) {
                        document.setPathInfo("/" + link.getPath());
                    } else {
                        document.setPathInfo(StringUtils.EMPTY);
                    }
                    document.setMountPath(link.getMount().getMountPath());
                    document.setHostName(link.getMount().getVirtualHost().getHostName());
                    document.setContextPath(link.getMount().getContextPath());

                    document.setCmsPreviewPrefix(link.getMount().getVirtualHost().getVirtualHosts().getCmsPreviewPrefix());
                    channelDocuments.add(document);

                }
            });

        }

        return channelDocuments;
    }

    @Override
    public List<ChannelDocument> getPreviewChannels(final Session userSession, final String hostGroup, final String uuid, final String branchId) {

        final HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            log.error("#getChannels invoked with a branchId should always originate from a real http request having an " +
                    "HstRequestContext on a ThreadLocal. RequestContext is missing, return #getChannels ignoring " +
                    "branchId '{}'", branchId);
        }
        requestContext.setAttribute(PREFER_RENDER_BRANCH_ID, branchId);
        return getPreviewChannels(userSession, hostGroup, uuid);
    }

    public String getUrl(final Session userSession, final String hostGroup, final String uuid, final String type) {

        final HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            log.error("#getUrl invoked without HstRequestContext but should always originate from a real http request " +
                    "having an HstRequestContext on a ThreadLocal. RequestContext is missing, return empty");
            return StringUtils.EMPTY;
        }


        final HstLink bestLink = getBestLink(userSession, hostGroup, uuid, type);
        if (bestLink == null) {
            return StringUtils.EMPTY;
        }
        return bestLink.toUrlForm(requestContext, true);

    }

    /**
     * returns best link for uuid & type combination and <code>null</code> if no node for uuid or no link can be made
     * method package private for unit testing
     */
    private HstLink getBestLink(final Session userSession, final String hostGroup, final String uuid, final String type) {
        Node handle = ResourceUtil.getNode(userSession, uuid);
        if (handle == null) {
            return null;
        }

        List<HstLink> canonicalLinks = new ArrayList<>();

        final Optional<HstModel> firstModel = hstModelRegistry.getModels().values().stream()
                .filter(m -> m.getVirtualHosts().getMountsByHostGroup(hostGroup).size() > 0).findFirst();
        firstModel.ifPresent(model -> {
            final Optional<Mount> mountCandidate = model.getVirtualHosts().getMountsByHostGroup(hostGroup).stream().findFirst();
            mountCandidate.ifPresent(mount -> canonicalLinks.addAll(model.getHstLinkCreator().createAllAvailableCanonicals(handle, mount, null, hostGroup)));
        });

        if (canonicalLinks.isEmpty()) {
            log.info("Cannot generate URL of type '{}' for document with UUID '{}' because no mount in the host group '{}' matches",
                    new Object[]{type, uuid, hostGroup});

            return null;
        }

        // Determine the 'best' canonical link: the one whose mount has the closest content path
        // {@link Mount#getContentPath()} to the path of the document handle. If multiple {@link Mount}'s have
        // an equally well suited {@link Mount#getContentPath()}, we pick the mount with the fewest types.
        // These mounts are in general the most generic ones.
        // If this still results in multiple mounts, we pick the one which has the most ancestors : The deeper the mountPath,
        // the more specific the mount can be considered
        //
        // If multiple {@link Mount}'s have equally well suited
        // {@link Mount#getCanonicalContentPath()}, an equal number of types and equal number of ancestors, we pick one at random.
        List<HstLink> candidateLinks = new ArrayList<HstLink>();
        int bestPathLength = 0;
        for (HstLink link : canonicalLinks) {
            Mount mount = link.getMount();

            if (mount.getContentPath().length() == bestPathLength) {
                // equally well as already found ones. Add to the candidates.
                candidateLinks.add(link);
            } else if (mount.getContentPath().length() > bestPathLength) {
                // this is a better one than the ones already found. Clear the candidates first.
                candidateLinks.clear();
                candidateLinks.add(link);
                bestPathLength = mount.getContentPath().length();
            } else {
                // ignore, we already have a better link
            }
        }

        List<HstLink> secondCandidateList = new ArrayList<HstLink>();
        int typeCount = 0;
        for (HstLink link : candidateLinks) {
            if (secondCandidateList.isEmpty()) {
                secondCandidateList.add(link);
                typeCount = link.getMount().getTypes().size();
                continue;
            }
            final Mount mount = link.getMount();
            if (mount.getTypes().size() < typeCount) {
                secondCandidateList.clear();
                typeCount = mount.getTypes().size();
                secondCandidateList.add(link);
            } else if (mount.getTypes().size() == typeCount) {
                secondCandidateList.add(link);
            }
        }

        HstLink bestLink = secondCandidateList.get(0);
        if (secondCandidateList.size() > 1) {
            for (HstLink link : secondCandidateList) {
                if (link == bestLink) {
                    continue;
                }
                if (hasLinkMoreMountAncestorsThanBestLink(link, bestLink)) {
                    bestLink = link;
                }
            }
        }
        return bestLink;
    }

    private boolean hasLinkMoreMountAncestorsThanBestLink(final HstLink testLink, final HstLink bestLink) {
        int nrOfAncestorsTestLink = getNumberOfMountAncestors(testLink);
        int nrOfAncestorsBestLink = getNumberOfMountAncestors(bestLink);
        return nrOfAncestorsTestLink > nrOfAncestorsBestLink;
    }

    private int getNumberOfMountAncestors(final HstLink testLink) {
        Mount current = testLink.getMount();
        int counter = 0;
        while (current.getParent() != null) {
            current = current.getParent();
            counter++;
        }
        return counter;
    }

}

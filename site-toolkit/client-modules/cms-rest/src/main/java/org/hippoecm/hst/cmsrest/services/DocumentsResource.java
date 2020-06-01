/*
*  Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.rest.DocumentService;
import org.hippoecm.hst.rest.beans.ChannelDocument;
import org.hippoecm.hst.rest.beans.ChannelDocumentDataset;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.RENDER_BRANCH_ID;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

public class DocumentsResource extends BaseResource implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentsResource.class);

    private HstLinkCreator hstLinkCreator;
    // default no context augmenters
    private List<DocumentContextAugmenter> documentContextAugmenters = new ArrayList<>();

    public void setHstLinkCreator(HstLinkCreator hstLinkCreator) {
        this.hstLinkCreator = hstLinkCreator;
    }

    public void addDocumentContextAugmenter(final DocumentContextAugmenter documentContextAugmenter) {
        documentContextAugmenters.add(documentContextAugmenter);
    }

    public ChannelDocumentDataset getChannels(String uuid) {
        return getChannels(uuid, MASTER_BRANCH_ID);
    }

    @Override
    public ChannelDocumentDataset getChannels(final String uuid, final String branchId) {
        final ChannelDocumentDataset dataset = new ChannelDocumentDataset();

        HstRequestContext requestContext = RequestContextProvider.get();

        if (!MASTER_BRANCH_ID.equals(branchId)) {
            // use a specific branch to find the right channel documents (this attribute is used in downstream projects)
            // to select a specific branch from CompositeHstSiteImpl
            // TODO in version 13 for hst-platform we need to come up with something else since then there will be no
            // TODO request context (possibly)
            requestContext.setAttribute(RENDER_BRANCH_ID, branchId);
        }

        documentContextAugmenters.forEach(dca -> dca.apply(requestContext, uuid));

        Node handle = ResourceUtil.getNode(requestContext, uuid);
        if (handle == null) {
            return dataset;
        }

        List<HstLink> canonicalLinks = hstLinkCreator.createAllAvailableCanonicals(handle, requestContext, null, getHostGroupNameForCmsHost());
        List<ChannelDocument> channelDocuments = new ArrayList<>(canonicalLinks.size());

        for (HstLink link : canonicalLinks) {
            final Mount linkMount = link.getMount();
            final Channel channel = linkMount.getChannel();

            if (channel == null) {
                log.debug("Skipping link for mount '{}' since it does not have a channel", linkMount.getName());
                continue;
            }

            if (!channelFilter.apply(channel)) {
                log.info("Skipping channel '{}' because filtered out by channel filters", channel);
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

            // The preview in the cms always accesses the hst site through the hostname of the cms, but
            // adds the contextpath of the website.
            if (link.getMount().getContextPath() != null) {
                document.setContextPath(link.getMount().getContextPath());
            } else {
                // if there is no contextpath configured on the Mount belonging to the HstLink, we use the contextpath
                // from the current HttpServletRequest
                document.setContextPath(requestContext.getServletRequest().getContextPath());
            }

            // set the cmsPreviewPrefix through which prefix after the contextPath the channels can be accessed
            document.setCmsPreviewPrefix(link.getMount().getVirtualHost().getVirtualHosts().getCmsPreviewPrefix());

            channelDocuments.add(document);

        }

        dataset.setChannelDocuments(channelDocuments);
        return dataset;

    }

    public String getUrl(final String uuid, final String type) {
        if (hstLinkCreator == null) {
            log.warn("Cannot generate URL of type '{}' for document with UUID '{}' because hstLinkCreator is null", type, uuid);
            return "";
        }

        HstRequestContext requestContext = RequestContextProvider.get();
        final MutableResolvedMount resolvedMount = (MutableResolvedMount) requestContext.getResolvedMount();
        final Mount previewDecoratedMount = resolvedMount.getMount();
        if (Mount.LIVE_NAME.equals(type)) {
            try {
                // although this is a cms request, the links that are requested now are links meant to share, like in
                // 'twitter', 'facebook', 'linkedin' or for example the news letter manager. Hence, during url creation, we
                // temporarily switch off that the request is a cms request
                ((HstMutableRequestContext) requestContext).setCmsRequest(false);
                final Mount unDecoratedMount = (Mount) requestContext.getAttribute(ContainerConstants.UNDECORATED_MOUNT);
                resolvedMount.setMount(unDecoratedMount);
                HstLink bestLink = getBestLink(uuid, type);
                if (bestLink == null) {
                    return "";
                }
                return bestLink.toUrlForm(requestContext, true);

            } finally {
                // switch cms context back
                ((HstMutableRequestContext) requestContext).setCmsRequest(true);
                resolvedMount.setMount(previewDecoratedMount);
            }
        } else {
            HstLink bestLink = getBestLink(uuid, type);
            if (bestLink == null) {
                return "";
            }
            return bestLink.toUrlForm(requestContext, true);
        }

    }

    /**
     * returns best link for uuid & type combination and <code>null</code> if no node for uuid or no link can be made
     * method package private for unit testing
     */
    HstLink getBestLink(final String uuid, final String type) {
        HstRequestContext requestContext = RequestContextProvider.get();
        Node handle = ResourceUtil.getNode(requestContext, uuid);
        if (handle == null) {
            return null;
        }
        final String hostGroupNameForCmsHost = getHostGroupNameForCmsHost();
        List<HstLink> canonicalLinks = hstLinkCreator.createAllAvailableCanonicals(handle, requestContext, type, hostGroupNameForCmsHost);

        if (canonicalLinks.isEmpty()) {
            log.info("Cannot generate URL of type '{}' for document with UUID '{}' because no mount in the host group '{}' matches",
                    new Object[]{type, uuid, hostGroupNameForCmsHost});

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

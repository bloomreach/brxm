/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueprintService implements Blueprint {

    final static Logger log = LoggerFactory.getLogger(BlueprintService.class);

    public static final String SUBSITE_TEMPLATES_PATH = "/hippo:configuration/hippo:queries/hippo:templates/new-subsite/hippostd:templates/";

    private final String id;
    private final String name;
    private final String description;
    private final String path;
    private final boolean hasContentPrototype;

    private final Channel prototypeChannel;

    public BlueprintService(final Node blueprint) throws RepositoryException {
        path = blueprint.getPath();

        id = blueprint.getName();

        if (blueprint.hasProperty(HstNodeTypes.BLUEPRINT_PROPERTY_NAME)) {
            this.name = blueprint.getProperty(HstNodeTypes.BLUEPRINT_PROPERTY_NAME).getString();
        } else {
            this.name = this.id;
        }

        if (blueprint.hasProperty(HstNodeTypes.BLUEPRINT_PROPERTY_DESCRIPTION)) {
            this.description = blueprint.getProperty(HstNodeTypes.BLUEPRINT_PROPERTY_DESCRIPTION).getString();
        } else {
            this.description = null;
        }

        if (blueprint.hasNode(HstNodeTypes.NODENAME_HST_CHANNEL)) {
            this.prototypeChannel = ChannelPropertyMapper.readChannel(blueprint.getNode(HstNodeTypes.NODENAME_HST_CHANNEL), null);
        } else {
            this.prototypeChannel = new Channel((String) null);
        }

        hasContentPrototype = blueprint.getSession().itemExists(SUBSITE_TEMPLATES_PATH + this.id);

        final boolean hasSite = readSite(blueprint);
        readMount(blueprint, hasSite);
    }

    private boolean readSite(final Node blueprint) throws RepositoryException {
        if (blueprint.hasNode(HstNodeTypes.NODENAME_HST_SITE)) {
            final Node siteNode = blueprint.getNode(HstNodeTypes.NODENAME_HST_SITE);

            if (siteNode.hasProperty(HstNodeTypes.SITE_CONFIGURATIONPATH)) {
                this.prototypeChannel.setHstConfigPath(siteNode.getProperty(HstNodeTypes.SITE_CONFIGURATIONPATH).getString());
            }
            if (siteNode.hasNode(HstNodeTypes.NODENAME_HST_CONTENTNODE) && !hasContentPrototype) {
                final Node contentNode = siteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
                final String docbase = contentNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();

                // assumption: docbase is always a UUID
                try {
                    Node ref = contentNode.getSession().getNodeByIdentifier(docbase);
                    this.prototypeChannel.setContentRoot(ref.getPath());
                } catch (ItemNotFoundException e) {
                    log.warn("Blueprint '{}' contains a site node with a broken content root reference (UUID='{}'). This content root will be ignored.",
                            blueprint.getPath(), docbase);
                }
            }
            return true;
        }
        return false;
    }

    private void readMount(final Node blueprint, final boolean hasSite) throws RepositoryException {
        if (blueprint.hasNode(HstNodeTypes.NODENAME_HST_MOUNT)) {
            final Node prototypeMount = blueprint.getNode(HstNodeTypes.NODENAME_HST_MOUNT);
            if (prototypeMount.hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
                final String mountPoint = prototypeMount.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).getString();
                if (hasSite) {
                    log.warn("Blueprint '{}' will ignore the static mount point '{}' because it also has a site node. Each channel created from this blueprint will therefore get a copy of the site node as its mount point.",
                            blueprint.getPath(), mountPoint);
                } else {
                    this.prototypeChannel.setHstMountPoint(mountPoint);
                }
            }
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public Channel createChannel() {
        return new Channel(prototypeChannel);
    }

    public Node getNode(final Session session) throws RepositoryException {
        return session.getNode(path);
    }

    @Override
    public boolean hasContentPrototype() {
        return hasContentPrototype;
    }

}

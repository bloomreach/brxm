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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueprintHandler {

    final static Logger log = LoggerFactory.getLogger(BlueprintHandler.class);

    public static final String SUBSITE_TEMPLATES_PATH = "/hippo:configuration/hippo:queries/hippo:templates/new-subsite/hippostd:templates/";

    public static Blueprint buildBlueprint(final Node blueprintNode) throws RepositoryException {
        Blueprint blueprint = new Blueprint();

        blueprint.setPath(blueprintNode.getPath());

        blueprint.setId(blueprintNode.getName());

        if (blueprintNode.hasProperty(HstNodeTypes.BLUEPRINT_PROPERTY_NAME)) {
            blueprint.setName(blueprintNode.getProperty(HstNodeTypes.BLUEPRINT_PROPERTY_NAME).getString());
        } else {
            blueprint.setName(blueprint.getId());
        }

        if (blueprintNode.hasProperty(HstNodeTypes.BLUEPRINT_PROPERTY_DESCRIPTION)) {
            blueprint.setDescription(blueprintNode.getProperty(HstNodeTypes.BLUEPRINT_PROPERTY_DESCRIPTION).getString());
        }

        if (blueprintNode.hasProperty(HstNodeTypes.BLUEPRINT_PROPERTY_CONTENT_ROOT)) {
            final String location = blueprintNode.getProperty(HstNodeTypes.BLUEPRINT_PROPERTY_CONTENT_ROOT).getString().trim();
            if(StringUtils.isEmpty(location) || !location.startsWith("/")) {
                log.warn("Skipping invalid '{}' of blueprint '{}' : The value should start with a / ", 
                        HstNodeTypes.BLUEPRINT_PROPERTY_CONTENT_ROOT, blueprint.getPath());
            } else {
                log.debug("Setting contentRoot for blueprint '{}' to '{}'", blueprint.getPath(), location);
                blueprint.setContentRoot(location);
            }
        }

        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNEL)) {
            blueprint.setPrototypeChannel(ChannelPropertyMapper.readChannel(blueprintNode.getNode(HstNodeTypes.NODENAME_HST_CHANNEL), null));
        } else {
            // TODO - What is that ? See if you can fix it!
            blueprint.setPrototypeChannel(new Channel((String) null));
        }

        blueprint.setHasContentPrototype(blueprintNode.getSession().itemExists(SUBSITE_TEMPLATES_PATH + blueprint.getId()));
        readMount(readSite(blueprintNode, blueprint), blueprintNode, blueprint);
        return blueprint;
    }

    private static boolean readSite(final Node blueprintNode, final Blueprint blueprint) throws RepositoryException {
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_SITE)) {
            final Node siteNode = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_SITE);

            if (siteNode.hasProperty(HstNodeTypes.SITE_CONFIGURATIONPATH)) {
                log.warn("Property '{}'  on '{}' has been deprecated. Make sure the blueprint has its own hst:configuration prototype.",
                        HstNodeTypes.SITE_CONFIGURATIONPATH, siteNode.getPath());
            }

            if (!blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_CONFIGURATION)) {  // validate that blueprint is correct
                throw new ItemNotFoundException("Blueprint "+blueprint.getId()+" does not have the mandatory node 'hst:configuration'");
            }

            if (siteNode.hasNode(HstNodeTypes.NODENAME_HST_CONTENTNODE) && !blueprint.getHasContentPrototype()) {
                final Node contentNode = siteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
                final String docbase = contentNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();

                // Assumption: docbase is always a UUID
                try {
                    Node ref = contentNode.getSession().getNodeByIdentifier(docbase);
                    blueprint.getPrototypeChannel().setContentRoot(ref.getPath());
                } catch (ItemNotFoundException e) {
                    log.warn("Blueprint '{}' contains a site node with a broken content root reference (UUID='{}'). This content root will be ignored.",
                            blueprintNode.getPath(), docbase);
                }
            }
            return true;
        }
        return false;
    }

    private static void readMount(final boolean hasSite, final Node blueprintNode, final Blueprint blueprint) throws RepositoryException {
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_MOUNT)) {
            final Node prototypeMount = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_MOUNT);
            if (prototypeMount.hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
                final String mountPoint = prototypeMount.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).getString();
                if (hasSite) {
                    log.warn("Blueprint '{}' will ignore the static mount point '{}' because it also has a site node. Each channel created from this blueprint will therefore get a copy of the site node as its mount point.",
                            blueprintNode.getPath(), mountPoint);
                } else {
                    blueprint.getPrototypeChannel().setHstMountPoint(mountPoint);
                }
            }
            if (prototypeMount.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE)) {
                blueprint.getPrototypeChannel().setLocale(prototypeMount.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE).getString());
            }
        }
    }

    public static Node getNode(final Session session, final Blueprint blueprint) throws RepositoryException {
        return session.getNode(blueprint.getPath());
    }

}

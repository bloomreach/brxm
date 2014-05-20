/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueprintHandler {

    final static Logger log = LoggerFactory.getLogger(BlueprintHandler.class);

    public static final String SUBSITE_TEMPLATES_PATH = "/hippo:configuration/hippo:queries/hippo:templates/new-subsite/hippostd:templates/";

    public static Blueprint buildBlueprint(final HstNode blueprintNode) {
        Blueprint blueprint = new Blueprint();

        blueprint.setPath(blueprintNode.getValueProvider().getPath());

        blueprint.setId(blueprintNode.getName());

        if (blueprintNode.getValueProvider().hasProperty(HstNodeTypes.BLUEPRINT_PROPERTY_NAME)) {
            blueprint.setName(blueprintNode.getValueProvider().getString(HstNodeTypes.BLUEPRINT_PROPERTY_NAME));
        } else {
            blueprint.setName(blueprint.getId());
        }

        if (blueprintNode.getValueProvider().hasProperty(HstNodeTypes.BLUEPRINT_PROPERTY_DESCRIPTION)) {
            blueprint.setDescription(blueprintNode.getValueProvider().getString(HstNodeTypes.BLUEPRINT_PROPERTY_DESCRIPTION));
        }

        if (blueprintNode.getValueProvider().hasProperty(HstNodeTypes.BLUEPRINT_PROPERTY_CONTENT_ROOT)) {
            final String location = blueprintNode.getValueProvider().getString(HstNodeTypes.BLUEPRINT_PROPERTY_CONTENT_ROOT).trim();
            if(StringUtils.isEmpty(location) || !location.startsWith("/")) {
                log.warn("Skipping invalid '{}' of blueprint '{}' : The value should start with a / ", 
                        HstNodeTypes.BLUEPRINT_PROPERTY_CONTENT_ROOT, blueprint.getPath());
            } else {
                log.debug("Setting contentRoot for blueprint '{}' to '{}'", blueprint.getPath(), location);
                blueprint.setContentRoot(location);
            }
        }

        HstNode channelNode = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_CHANNEL);
        if (channelNode != null) {
            blueprint.setPrototypeChannel(ChannelPropertyMapper.readChannel(channelNode, null));
        } else {
            blueprint.setPrototypeChannel(new Channel());
        }

        readMount(readSite(blueprintNode, blueprint), blueprintNode, blueprint);

        return blueprint;
    }

    private static boolean readSite(final HstNode blueprintNode, final Blueprint blueprint) {
        HstNode blueprintSiteNode = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_SITE);
        if (blueprintSiteNode == null) {
            return false;
        }

        if (blueprintSiteNode.getValueProvider().hasProperty(HstNodeTypes.SITE_CONFIGURATIONPATH)) {
            blueprint.getPrototypeChannel().setHstConfigPath(blueprintSiteNode.getValueProvider().getString(HstNodeTypes.SITE_CONFIGURATIONPATH));
        } else if (blueprintNode.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATION) == null) {  // validate that blueprint is correct
            throw new ModelLoadingException(String.format("Blueprint %s has neither a hst:configuration node prototype or a fixed hst:configurationpath", blueprint.getId()));
        }

        if (blueprintSiteNode.getValueProvider().hasProperty(HstNodeTypes.SITE_CONTENT)) {
            String siteContentPath = blueprintSiteNode.getValueProvider().getString(HstNodeTypes.SITE_CONTENT);
            blueprint.getPrototypeChannel().setContentRoot(siteContentPath);
        }

        return true;
    }

    private static void readMount(final boolean hasSite, final HstNode blueprintNode, final Blueprint blueprint) {
        final HstNode blueprintMount = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_MOUNT);
        if (blueprintMount != null) {
            final HstNode prototypeMount = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_MOUNT);
            if (prototypeMount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
                final String mountPoint = prototypeMount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT);
                if (hasSite) {
                    log.warn("Blueprint '{}' will ignore the static mount point '{}' because it also has a site node. Each channel created from this blueprint will therefore get a copy of the site node as its mount point.",
                            blueprintNode.getValueProvider().getPath(), mountPoint);
                } else {
                    blueprint.getPrototypeChannel().setHstMountPoint(mountPoint);
                }
            }
            if (prototypeMount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE)) {
                blueprint.getPrototypeChannel().setLocale(prototypeMount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCALE));
            }
        }
    }

}

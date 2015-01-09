/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesomeIconType;

/**
 * @version $Id$
 */
public class JcrNodeIcon {

    static final Logger log = LoggerFactory.getLogger(JcrNodeIcon.class);

    private final static Map<String, FontAwesomeIconType> nodeNameIcons;

    private final static List<String> nodeTypes;

    static {
        nodeTypes = new ArrayList<>();
        nodeTypes.add("hippostd:publishable");

        nodeNameIcons = new HashMap<>();
        // hst config
        nodeNameIcons.put("hst:hst", FontAwesomeIconType.cloud);
        nodeNameIcons.put("hst:sitemapitem", FontAwesomeIconType.sitemap);
        nodeNameIcons.put("hst:sitemap", FontAwesomeIconType.sitemap);
        nodeNameIcons.put("hst:template", FontAwesomeIconType.file_text_o);
        nodeNameIcons.put("hst:templates", FontAwesomeIconType.file_o);
        nodeNameIcons.put("hst:component", FontAwesomeIconType.puzzle_piece);
        nodeNameIcons.put("hst:components", FontAwesomeIconType.puzzle_piece);
        nodeNameIcons.put("hst:abstractcomponent", FontAwesomeIconType.puzzle_piece);
        nodeNameIcons.put("hst:blueprint", FontAwesomeIconType.dropbox);
        nodeNameIcons.put("hst:blueprints", FontAwesomeIconType.dropbox);
        nodeNameIcons.put("hst:channels", FontAwesomeIconType.laptop);
        nodeNameIcons.put("hst:channel", FontAwesomeIconType.laptop);
        nodeNameIcons.put("hst:channelinfo", FontAwesomeIconType.info_circle);
        nodeNameIcons.put("hst:sites", FontAwesomeIconType.star_o);
        nodeNameIcons.put("hst:site", FontAwesomeIconType.star);
        nodeNameIcons.put("hst:pages", FontAwesomeIconType.copy);
        nodeNameIcons.put("hst:workspace", FontAwesomeIconType.wrench);
        nodeNameIcons.put("hst:catalog", FontAwesomeIconType.dropbox);
        nodeNameIcons.put("hst:configuration", FontAwesomeIconType.cog);
        nodeNameIcons.put("hst:configurations", FontAwesomeIconType.cogs);
        nodeNameIcons.put("hst:mount", FontAwesomeIconType.rocket);
        nodeNameIcons.put("hst:virtualhosts", FontAwesomeIconType.arrow_circle_down);
        nodeNameIcons.put("hst:virtualhostgroup", FontAwesomeIconType.arrow_circle_o_right);
        nodeNameIcons.put("hst:virtualhost", FontAwesomeIconType.arrow_circle_right);
        nodeNameIcons.put("hippogallery:stdAssetGallery", FontAwesomeIconType.paperclip);
        nodeNameIcons.put("hippogallery:stdImageGallery", FontAwesomeIconType.picture_o);
        nodeNameIcons.put("webresources:webresources", FontAwesomeIconType.suitcase);
        nodeNameIcons.put("hst:containeritemcomponent", FontAwesomeIconType.puzzle_piece);
        nodeNameIcons.put("hst:containeritempackage", FontAwesomeIconType.dropbox);
        nodeNameIcons.put("hst:containercomponent", FontAwesomeIconType.columns);
        nodeNameIcons.put("hst:containercomponentfolder", FontAwesomeIconType.trello);
        nodeNameIcons.put("hst:sitemenus", FontAwesomeIconType.th_list);
        nodeNameIcons.put("hst:sitemenu", FontAwesomeIconType.th_list);
        nodeNameIcons.put("hst:sitemenuitem", FontAwesomeIconType.align_justify);

        // content
        nodeNameIcons.put("hippo:handle", FontAwesomeIconType.umbrella);
        nodeNameIcons.put("hippostd:publishable", FontAwesomeIconType.file_text);
        nodeNameIcons.put("hippo:translation", FontAwesomeIconType.flag_o);
        nodeNameIcons.put("hippotranslation:translations", FontAwesomeIconType.random);
        nodeNameIcons.put("hippostd:folder", FontAwesomeIconType.folder_o);
        nodeNameIcons.put("hippostd:directory", FontAwesomeIconType.folder);
        nodeNameIcons.put("hippofacnav:facetnavigation", FontAwesomeIconType.dribbble);
        nodeNameIcons.put("hst:formdatacontainer", FontAwesomeIconType.certificate);
        nodeNameIcons.put("hst:formdata", FontAwesomeIconType.certificate);

        // config
        nodeNameIcons.put("hipposys:configuration", FontAwesomeIconType.cogs);
        nodeNameIcons.put("hipposysedit:namespacefolder", FontAwesomeIconType.bullseye);
        nodeNameIcons.put("hipposysedit:namespace", FontAwesomeIconType.bullseye);
        nodeNameIcons.put("frontend:plugin", FontAwesomeIconType.cog);
        nodeNameIcons.put("frontend:pluginconfig", FontAwesomeIconType.cog);
        nodeNameIcons.put("frontend:plugincluster", FontAwesomeIconType.cogs);
        nodeNameIcons.put("editor:templateset", FontAwesomeIconType.file_text_o);
        nodeNameIcons.put("hipposysedit:prototypeset", FontAwesomeIconType.star_o);
        nodeNameIcons.put("hipposysedit:templatetype", FontAwesomeIconType.file_text);
    }

    public static IconType getIcon(Node jcrNode) {
        // types first
        for(String nodeType : nodeTypes) {
            if(isNodeType(jcrNode, nodeType)) {
                if(nodeNameIcons.containsKey(nodeType)) {
                    return nodeNameIcons.get(nodeType);
                }
            }
        }

        final String nodeName = getNodeName(jcrNode);
        if(nodeNameIcons.containsKey(nodeName)) {
            return nodeNameIcons.get(nodeName);
        }
        if (isVirtual(jcrNode)) {
            return getVirtualNodeIconType();
        }
        return getDefaultIconType();
    }

    private static String getNodeName(final Node jcrNode) {
        try {
            return jcrNode.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    public static boolean isNodeType(final Node jcrNode, final String typeName) {
        try {
            return jcrNode.isNodeType(typeName);
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if the wrapped jcr node is a virtual node
     * @return true if the node is virtual else false
     */
    public static boolean isVirtual(Node jcrNode) {
        try {
            return JcrUtils.isVirtual(jcrNode);
        } catch (RepositoryException e) {
            log.info("Cannot determine whether node '{}' is virtual, assuming it's not", JcrUtils.getNodePathQuietly(jcrNode), e);
            return false;
        }
    }

    public static IconType getDefaultIconType() {
        return FontAwesomeIconType.circle;
    }

    public static IconType getVirtualNodeIconType() {
        return FontAwesomeIconType.circle_o;
    }

    private JcrNodeIcon() {}
}

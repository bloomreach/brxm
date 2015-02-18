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
package org.hippoecm.frontend.plugins.console.browser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.CONFIGURATION_PATH;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.NT_DIRECTORY;
import static org.hippoecm.repository.HippoStdNodeType.NT_FOLDER;

public class JcrNodeIcon {

    static final Logger log = LoggerFactory.getLogger(JcrNodeIcon.class);

    private static final Map<String, FontAwesomeIconClass> nodeNameIcons;
    private static final List<String> nodeTypes;
    private static final Map<String, String> pathCssNames;
    private static final String JCRNODE_CSSNAME_PREFIX = "jcrnode-";
    private static final String JCRNODE_CSSNAME_DEFAULT = "default";
    private static final String FA_CSSCLASS_PREFIX = "fa ";
    private static final String FA_DEFAULT_ICON_CSSNAME = FontAwesomeIconClass.CIRCLE.cssClassName();
    private static final String FA_VIRTUALNODE_ICON_CSSNAME = FontAwesomeIconClass.CIRCLE_O.cssClassName();

    public static final String DEFAULTNODE_ICON_CSSCLASS = FA_CSSCLASS_PREFIX + " " + FA_DEFAULT_ICON_CSSNAME + " "
            + JCRNODE_CSSNAME_PREFIX + JCRNODE_CSSNAME_DEFAULT;

    static {
        nodeTypes = new ArrayList<>();
        nodeTypes.add("hippostd:publishable");

        nodeNameIcons = new HashMap<>();
        // hst config
        nodeNameIcons.put("hst:hst", FontAwesomeIconClass.CLOUD);
        nodeNameIcons.put("hst:sitemapitem", FontAwesomeIconClass.SITEMAP);
        nodeNameIcons.put("hst:sitemap", FontAwesomeIconClass.SITEMAP);
        nodeNameIcons.put("hst:template", FontAwesomeIconClass.FILE_TEXT_O);
        nodeNameIcons.put("hst:templates", FontAwesomeIconClass.FILE_O);
        nodeNameIcons.put("hst:component", FontAwesomeIconClass.PUZZLE_PIECE);
        nodeNameIcons.put("hst:components", FontAwesomeIconClass.PUZZLE_PIECE);
        nodeNameIcons.put("hst:abstractcomponent", FontAwesomeIconClass.PUZZLE_PIECE);
        nodeNameIcons.put("hst:blueprint", FontAwesomeIconClass.DROPBOX);
        nodeNameIcons.put("hst:blueprints", FontAwesomeIconClass.DROPBOX);
        nodeNameIcons.put("hst:channels", FontAwesomeIconClass.LAPTOP);
        nodeNameIcons.put("hst:channel", FontAwesomeIconClass.LAPTOP);
        nodeNameIcons.put("hst:channelinfo", FontAwesomeIconClass.INFO_CIRCLE);
        nodeNameIcons.put("hst:sites", FontAwesomeIconClass.STAR_O);
        nodeNameIcons.put("hst:site", FontAwesomeIconClass.STAR);
        nodeNameIcons.put("hst:pages", FontAwesomeIconClass.COPY);
        nodeNameIcons.put("hst:workspace", FontAwesomeIconClass.WRENCH);
        nodeNameIcons.put("hst:catalog", FontAwesomeIconClass.DROPBOX);
        nodeNameIcons.put("hst:configuration", FontAwesomeIconClass.COG);
        nodeNameIcons.put("hst:configurations", FontAwesomeIconClass.COGS);
        nodeNameIcons.put("hst:mount", FontAwesomeIconClass.ROCKET);
        nodeNameIcons.put("hst:virtualhosts", FontAwesomeIconClass.ARROW_CIRCLE_DOWN);
        nodeNameIcons.put("hst:virtualhostgroup", FontAwesomeIconClass.ARROW_CIRCLE_O_RIGHT);
        nodeNameIcons.put("hst:virtualhost", FontAwesomeIconClass.ARROW_CIRCLE_RIGHT);
        nodeNameIcons.put("hippogallery:stdAssetGallery", FontAwesomeIconClass.PAPERCLIP);
        nodeNameIcons.put("hippogallery:stdImageGallery", FontAwesomeIconClass.PICTURE_O);
        nodeNameIcons.put("hst:containeritemcomponent", FontAwesomeIconClass.CUBE);
        nodeNameIcons.put("hst:containeritempackage", FontAwesomeIconClass.DROPBOX);
        nodeNameIcons.put("hst:containercomponent", FontAwesomeIconClass.COLUMNS);
        nodeNameIcons.put("hst:containercomponentfolder", FontAwesomeIconClass.TRELLO);
        nodeNameIcons.put("hst:sitemenus", FontAwesomeIconClass.TH_LIST);
        nodeNameIcons.put("hst:sitemenu", FontAwesomeIconClass.TH_LIST);
        nodeNameIcons.put("hst:sitemenuitem", FontAwesomeIconClass.ALIGN_JUSTIFY);

        // content
        nodeNameIcons.put("hippo:handle", FontAwesomeIconClass.UMBRELLA);
        nodeNameIcons.put("hippostd:publishable", FontAwesomeIconClass.FILE_TEXT);
        nodeNameIcons.put("hippo:translation", FontAwesomeIconClass.FLAG_O);
        nodeNameIcons.put("hippotranslation:translations", FontAwesomeIconClass.RANDOM);
        nodeNameIcons.put(NT_FOLDER, FontAwesomeIconClass.FOLDER_O);
        nodeNameIcons.put(NT_DIRECTORY, FontAwesomeIconClass.FOLDER);
        nodeNameIcons.put("hippofacnav:facetnavigation", FontAwesomeIconClass.DRIBBBLE);
        nodeNameIcons.put("hst:formdatacontainer", FontAwesomeIconClass.SEND);
        nodeNameIcons.put("hst:formdata", FontAwesomeIconClass.SEND_O);

        // config
        nodeNameIcons.put("webfiles:webfiles", FontAwesomeIconClass.SUITCASE);
        nodeNameIcons.put("webfiles:bundle", FontAwesomeIconClass.SUITCASE);
        nodeNameIcons.put("nt:folder", FontAwesomeIconClass.FOLDER_O);
        nodeNameIcons.put("nt:file", FontAwesomeIconClass.FILE_TEXT_O);
        nodeNameIcons.put("hipposys:configuration", FontAwesomeIconClass.COGS);
        nodeNameIcons.put("hipposysedit:namespacefolder", FontAwesomeIconClass.BULLSEYE);
        nodeNameIcons.put("hipposysedit:namespace", FontAwesomeIconClass.BULLSEYE);
        nodeNameIcons.put("frontend:plugin", FontAwesomeIconClass.PLUG);
        nodeNameIcons.put("frontend:pluginconfig", FontAwesomeIconClass.COG);
        nodeNameIcons.put("frontend:plugincluster", FontAwesomeIconClass.COGS);
        nodeNameIcons.put("editor:templateset", FontAwesomeIconClass.FILE_TEXT_O);
        nodeNameIcons.put("hipposysedit:prototypeset", FontAwesomeIconClass.STAR_O);
        nodeNameIcons.put("hipposysedit:templatetype", FontAwesomeIconClass.FILE_TEXT);
        nodeNameIcons.put("hipposys:group", FontAwesomeIconClass.USERS);
        nodeNameIcons.put("hipposys:groupfolder", FontAwesomeIconClass.USERS);
        nodeNameIcons.put("hipposys:userfolder", FontAwesomeIconClass.USER);
        nodeNameIcons.put("hipposys:user", FontAwesomeIconClass.USER);
        nodeNameIcons.put("hipposys:workflowfolder", FontAwesomeIconClass.REFRESH);
        nodeNameIcons.put("hipposys:workflowcategory", FontAwesomeIconClass.REFRESH);
        nodeNameIcons.put("hipposys:workflow", FontAwesomeIconClass.REFRESH);
        nodeNameIcons.put("hipposys:update", FontAwesomeIconClass.WRENCH);
        nodeNameIcons.put("hipposys:applicationfolder", FontAwesomeIconClass.DIAMOND);
        nodeNameIcons.put("hipposys:modulefolder", FontAwesomeIconClass.SIMPLYBUILT);
        nodeNameIcons.put("hipposys:queryfolder", FontAwesomeIconClass.QUESTION_CIRCLE);
        nodeNameIcons.put("hipposys:queryfolder", FontAwesomeIconClass.QUESTION_CIRCLE);
        nodeNameIcons.put("hippostd:templatequery", FontAwesomeIconClass.QUESTION);

        pathCssNames = new HashMap<>();
        pathCssNames.put("/hst:hst", "hst");
        pathCssNames.put("/" + CONFIGURATION_PATH, "conf");
        pathCssNames.put("/content", "content");
        pathCssNames.put("/hippo:namespaces", "namespaces");
        pathCssNames.put("/formdata", "formdata");
        pathCssNames.put("/webfiles", "webfiles");
        pathCssNames.put("/hippo:reports", "reports");
        pathCssNames.put("/hippo:log", "log");

    }

    public static String getIconCssClass(Node jcrNode) {
        return FA_CSSCLASS_PREFIX + " " + getIconTypeCssClass(jcrNode) + " " + getIconColorCssClassname(jcrNode);
    }

    private static String getIconTypeCssClass(final Node jcrNode) {
        // types first
        for(String nodeType : nodeTypes) {
            if(isNodeType(jcrNode, nodeType)) {
                if(nodeNameIcons.containsKey(nodeType)) {
                    return nodeNameIcons.get(nodeType).cssClassName();
                }
            }
        }

        final String nodeName = getNodeName(jcrNode);
        if(nodeNameIcons.containsKey(nodeName)) {
            return nodeNameIcons.get(nodeName).cssClassName();
        }
        if (isVirtual(jcrNode)) {
            return FA_VIRTUALNODE_ICON_CSSNAME;
        }
        return FA_DEFAULT_ICON_CSSNAME;
    }

    private static String getIconColorCssClassname(final Node jcrNode) {
        String cssClassName = JCRNODE_CSSNAME_DEFAULT;
        try {
            final String path = jcrNode.getPath();

            if (path.startsWith("/content") && jcrNode.hasProperty(HIPPOSTD_STATE)) {
                cssClassName = jcrNode.getProperty(HIPPOSTD_STATE).getString();
            } else if (JcrNodeIcon.isNodeType(jcrNode, "hippofacnav:facetnavigation")) {
                cssClassName = "facnav";
            } else if (isVirtual(jcrNode)) {
                cssClassName = "virtual";
            }
            if(cssClassName.equals(JCRNODE_CSSNAME_DEFAULT)) {
                for (Map.Entry<String, String> pathColor : pathCssNames.entrySet()) {
                    if (path.startsWith(pathColor.getKey())) {
                        cssClassName = pathColor.getValue();
                        break;
                    }
                }
            }
        } catch (RepositoryException e) {
            // ignore, use default color
        }
        return JCRNODE_CSSNAME_PREFIX + cssClassName;
    }

    private static String getNodeName(final Node jcrNode) {
        try {
            return jcrNode.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private static boolean isNodeType(final Node jcrNode, final String typeName) {
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
    private static boolean isVirtual(Node jcrNode) {
        try {
            return JcrUtils.isVirtual(jcrNode);
        } catch (RepositoryException e) {
            log.info("Cannot determine whether node '{}' is virtual, assuming it's not", JcrUtils.getNodePathQuietly(jcrNode), e);
            return false;
        }
    }

    private JcrNodeIcon() {}
}

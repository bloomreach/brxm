/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.cms.dashboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.cms.dashboard.current.CurrentActivityPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentEvent {

    private static final Logger log = LoggerFactory.getLogger(CurrentActivityPlugin.class);
    private static final Pattern pattern = Pattern.compile("document\\[(?:(?:(?:uuid=([0-9a-fA-F-]+))|(?:path='(/[^']*)')),?)*\\]");

    private final Node node;

    private String sourceVariant;
    private boolean sourceVariantExists;
    private String targetVariant;
    private boolean targetVariantExists;

    public DocumentEvent(Node node) {
        this.node = node;
        try {
            Session session = node.getSession();
            initSourceVariant(node, session);
            initTargetVariant(node, session);
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void initTargetVariant(final Node node, final Session session) throws RepositoryException {
        targetVariant = JcrUtils.getStringProperty(node, "hippolog:eventReturnValue", null);
        targetVariantExists = false;
        if (targetVariant != null) {
            Matcher matcher = pattern.matcher(targetVariant);
            if (matcher.matches()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String patternElement = matcher.group(i);
                    if (patternElement.startsWith("/")) {
                        targetVariant = patternElement;
                    } else {
                        String path = uuid2Path(patternElement);
                        if (path != null && !path.isEmpty()) {
                            if (targetVariantExists = session.itemExists(path)) {
                                targetVariant = path;
                                break;
                            }
                        }
                    }
                }
            } else if (targetVariant.startsWith("/")) {
                try {
                    targetVariantExists = session.itemExists(targetVariant);
                } catch (RepositoryException e) {
                    targetVariantExists = false;
                }
            } else {
                targetVariant = null;
                targetVariantExists = false;
            }

            if (targetVariantExists) {
                Node target = session.getNode(targetVariant);
                if (target instanceof Version) {
                    Version version = (Version) target;
                    VersionHistory containingHistory = version.getContainingHistory();
                    String versionableIdentifier = containingHistory.getVersionableIdentifier();
                    String path = uuid2Path(versionableIdentifier);
                    if (path != null && !path.isEmpty()) {
                        if (targetVariantExists = session.itemExists(path)) {
                            targetVariant = path;
                        }
                    } else {
                        targetVariantExists = false;
                        targetVariant = null;
                    }
                }
            }
        } else if (sourceVariantExists && node.getProperty("hippolog:eventMethod").getString().equals("delete")
                && node.hasProperty("hippolog:eventArguments")
                && node.getProperty("hippolog:eventArguments").getValues().length > 0) {
            targetVariant = sourceVariant + "/"
                    + node.getProperty("hippolog:eventArguments").getValues()[0].getString();
            targetVariantExists = false;
        }
    }

    private void initSourceVariant(final Node node, final Session session) throws RepositoryException {
        sourceVariant = JcrUtils.getStringProperty(node, "hippolog:eventDocument", null);
        sourceVariantExists = sourceVariant != null && session.itemExists(sourceVariant);
    }

    public String getMethod() {
        try {
            return JcrUtils.getStringProperty(node, "hippolog:eventMethod", null);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private String getArgument(int index) {
        try {
            if (node.hasProperty("hippolog:eventArguments")
                    && node.getProperty("hippolog:eventArguments").getValues().length > index) {
                return node.getProperty("hippolog:eventArguments").getValues()[index].getString();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public IModel<String> getName() {
        final String argument = getArgument(0);
        final String method = getMethod();
        if ("delete".equals(method) && argument != null) {
            return new Model<String>(argument);
        } else if ("add".equals(method)) {
            return new NodeTranslator(new JcrNodeModel(targetVariant)).getNodeName();
        } else if ("addTranslation".equals(method)) {
            return new NodeTranslator(new JcrNodeModel(sourceVariant)).getNodeName();
        } else {
            String path = getDocumentPath();
            if (path != null) {
                return new NodeTranslator(new JcrNodeModel(path)).getNodeName();
            } else {
                return null;
            }
        }
    }

    public String getDocumentPath() {
        //Try to create a link to the document variant
        String path = null;
        if (targetVariantExists) {
            path = targetVariant;
        } else if (sourceVariantExists) {
            path = sourceVariant;
        }

        if (path != null) {
            return path;
        } else {
            // Maybe both variants have been deleted, try to create a link to the handle
            String handle;
            if (targetVariant != null) {
                handle = StringUtils.substringBeforeLast(targetVariant, "/");
            } else if (sourceVariant != null) {
                handle = StringUtils.substringBeforeLast(sourceVariant, "/");
            } else {
                handle = null;
            }
            if (handle != null) {
                return handle;
            }
        }
        return null;
    }

    private String uuid2Path(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }
        try {
            Session session = UserSession.get().getJcrSession();
            Node node = session.getNodeByIdentifier(uuid);
            return node.getPath();
        } catch (ItemNotFoundException ignore) {
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

}

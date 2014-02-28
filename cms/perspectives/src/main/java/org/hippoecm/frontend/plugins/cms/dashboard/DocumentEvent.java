/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.util.JcrUtils;

public class DocumentEvent {

    private static final Pattern pattern = Pattern.compile("document\\[(?:(?:(?:uuid=([0-9a-fA-F-]+))|(?:path='(/[^']*)')),?)*\\]");

    private final Node node;

    private String sourceVariant;
    private boolean sourceVariantExists;
    private String targetVariant;
    private boolean targetVariantExists;

    public DocumentEvent(Node node) throws RepositoryException {
        this.node = node;
        Session session = node.getSession();
        initSourceVariant(node, session);
        initTargetVariant(node, session);
    }

    private void initTargetVariant(final Node node, final Session session) throws RepositoryException {
        targetVariant = JcrUtils.getStringProperty(node, "hippolog:returnValue", null);
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
                        if (!StringUtils.isEmpty(path) && session.itemExists(path)) {
                            targetVariantExists = true;
                            targetVariant = path;
                            break;
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
                    if (!StringUtils.isEmpty(path) && session.itemExists(path)) {
                        targetVariantExists = true;
                        targetVariant = path;
                    } else {
                        targetVariantExists = false;
                        targetVariant = null;
                    }
                }
            }
        } else if (sourceVariantExists && "delete".equals(JcrUtils.getStringProperty(node, "hippolog:methodName", null))
                && node.hasProperty("hippolog:arguments")
                && node.getProperty("hippolog:arguments").getValues().length > 0) {
            targetVariant = sourceVariant + "/"
                    + node.getProperty("hippolog:arguments").getValues()[0].getString();
            targetVariantExists = false;
        }
    }

    private void initSourceVariant(final Node node, final Session session) throws RepositoryException {
        final String eventDocument = JcrUtils.getStringProperty(node, "hippolog:documentPath", null);
        sourceVariant = fixPathForRequests(eventDocument);
        sourceVariantExists = !StringUtils.isEmpty(sourceVariant) && session.itemExists(sourceVariant);
    }

    public String getMethod() throws RepositoryException {
        return JcrUtils.getStringProperty(node, "hippolog:methodName", null);
    }

    private String getArgument(int index) throws RepositoryException {
        if (node.hasProperty("hippolog:arguments")
                && node.getProperty("hippolog:arguments").getValues().length > index) {
            return node.getProperty("hippolog:arguments").getValues()[index].getString();
        }
        return null;
    }

    public IModel<String> getName() throws RepositoryException {
        final String argument = getArgument(0);
        final String method = getMethod();
        if ("delete".equals(method) && argument != null) {
            return new Model<String>(argument);
        } else if ("add".equals(method)) {
            return new NodeTranslator(new JcrNodeModel(targetVariant)).getNodeName();
        } else if ("addTranslation".equals(method)) {
            return new NodeTranslator(new JcrNodeModel(sourceVariant)).getNodeName();
        } else if ("rename".equals(getMethod()) && argument != null) {
            return new Model<String>(argument);
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
        }

        // Maybe both variants have been deleted, try to create a link to the handle
        if (targetVariant != null) {
            return targetVariant;
        } else if (sourceVariant != null) {
            return sourceVariant;
        }

        return null;
    }

    private String uuid2Path(String uuid) throws RepositoryException {
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }
        try {
            Session session = UserSession.get().getJcrSession();
            Node node = session.getNodeByIdentifier(uuid);
            return node.getPath();
        } catch (ItemNotFoundException ignore) {
        }
        return null;
    }

    private String fixPathForRequests(String path) {
        if (path == null || !path.endsWith("hippo:request")) {
            return path;
        }

        String[] pathElements = path.split("/");
        StringBuilder newPath = new StringBuilder();

        // build new path, strip last element, eg "hippo:request"
        for (int i = 0; i < pathElements.length - 1; i++) {
            if (!pathElements[i].isEmpty()) {
                newPath.append("/").append(pathElements[i]);
            }
        }

        // add last part again to point to document
        if (pathElements.length > 1) {
            newPath.append("/").append(pathElements[pathElements.length - 2]);
        }

        return newPath.toString();
    }

}

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

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.ocm.JcrObject;
import org.hippoecm.frontend.plugins.cms.dashboard.current.CurrentActivityPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentEvent extends JcrObject {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CurrentActivityPlugin.class);

    public DocumentEvent(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public String getDocumentPath() {
        Node node = getNode();
        try {
            Session session = node.getSession();

            // Best effort algoritm to create a 'browse' link to a document.

            // The path to the document variant that was used as input for a Workflow step.
            String sourceVariant = null;
            boolean sourceVariantExists = false;
            if (node.hasProperty("hippolog:eventDocument")) {
                sourceVariant = node.getProperty("hippolog:eventDocument").getValue().getString();
                sourceVariantExists = session.itemExists(sourceVariant);
            }

            //The path to the document variant that was returned by a Workflow step.
            //Workflow steps can return a Document instance who's toString()
            //value is stored as 'Document[uuid=...]'
            String targetVariant = null;
            boolean targetVariantExists = false;
            if (node.hasProperty("hippolog:eventReturnValue")) {
                targetVariant = node.getProperty("hippolog:eventReturnValue").getValue().getString();
                Pattern pattern = Pattern
                        .compile("document\\[(?:(?:(?:uuid=([0-9a-fA-F-]+))|(?:path='(/[^']*)')),?)*\\]");
                Matcher matcher = pattern.matcher(targetVariant);
                if (matcher.matches()) {
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        String patternElement = matcher.group(i);
                        if (patternElement.startsWith("/")) {
                            targetVariant = patternElement;
                        } else {
                            String path = uuid2Path(patternElement);
                            if (path != null && !path.equals("")) {
                                targetVariantExists = session.itemExists(path);
                                if (targetVariantExists) {
                                    targetVariant = path;
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
            } else if (node.getProperty("hippolog:eventMethod").getString().equals("delete")
                    && node.hasProperty("hippolog:eventArguments")
                    && node.getProperty("hippolog:eventArguments").getValues().length > 0) {
                targetVariant = node.getProperty("hippolog:eventArguments").getValues()[0].getString();
                targetVariantExists = false;
            }

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
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    String uuid2Path(String uuid) {
        if (uuid == null || uuid.equals("")) {
            return null;
        }
        try {
            Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
            Node node = session.getNodeByUUID(uuid);
            return node.getPath();
        } catch (ItemNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected void processEvents(IObservationContext context, Iterator<? extends IEvent> events) {
        // TODO Auto-generated method stub

    }
}

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
package org.hippoecm.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;

public abstract class Element {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public abstract String getElementName();
    
    boolean excluded = false;

    public String getFullName() {
        return getElementName();
    }

    static class ContentElement extends Element {
        String name;
        String path;
        String file;
        Node previous;
        Node current;
        boolean dirty = false;

        ContentElement(String name, String path, String file, Node content) {
            this.name = name;
            this.path = path;
            this.file = file;
            this.previous = content;
        }
        ContentElement(Node node, String file) throws RepositoryException {
            this.name = null;
            this.path = node.getPath();
            this.file = file;
            this.current = node;
        }
        ContentElement(Node node) throws RepositoryException {
            this.name = node.getName();
            this.path = node.getPath();
            this.file = null;
            this.previous = null;
            this.current = node;
        }
        public void setDirty() {
            dirty = true;
        }
        public String getPath() {
            return path;
        }
        public Node getPrevious() throws RepositoryException {
            return previous;
        }
        public Node getCurrent() throws RepositoryException {
            if(current == null) {
                if(previous.getSession().getRootNode().hasNode(path.substring(1))) {
                    return (current = previous.getSession().getRootNode().getNode(path.substring(1)));
                } else {
                    return null;
                }
            } else {
                return current;
            }
        }
        public String getElementName() {
            return name;
        }
    }


    static class NamespaceElement extends Element {
        String prefix;
        String uri;
        String cnd;
        String file; // file containing the cnd
        List<NamespaceElement> nodetypes;
        Set<ContentElement> templates;

        NamespaceElement(String prefix, String uri, String cnd) {
            this.prefix = prefix;
            this.uri = uri;
            this.file = cnd;
        }

        NamespaceElement(Node node) throws RepositoryException {
            prefix = node.getName();
        }

        NamespaceElement(String prefix, String uri) {
            this.prefix = prefix;
            this.uri = uri;
        }
        public String getElementName() {
            return prefix;
        }
    }

    static class ProjectElement extends Element {
        String projectName;
        private Node projectNode;
        URL url;
        Set<Element> elements = new HashSet<Element>();
        public String getElementName() {
            return null;
        }
        public String getFullName() {
            return projectName;
        }
        ProjectElement() {
            projectName = "";
            url = null;
        }
        ProjectElement(URL url, Node projects) throws MalformedURLException, IOException, RepositoryException, NotExportableException {
            this.url = url;
            Manifest manifest = new Manifest(new URL(url, "META-INF/MANIFEST.MF").openStream());
            Attributes manifestAttributes = manifest.getMainAttributes();
            String manifestGroup = manifestAttributes.getValue("Implementation-Vendor-Id");
            String manifestVersion = manifestAttributes.getValue("Implementation-Version");
            String manifestTitle = manifestAttributes.getValue("Implementation-Title");
            InputStream project = url.openStream();
            ((HippoSession)projects.getSession()).importDereferencedXML(projects.getPath(), project, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW, ImportMergeBehavior.IMPORT_MERGE_THROW);
            if(!projects.hasNode("hippo:initialize") || !projects.getNode("hippo:initialize").isNodeType("hippo:initializefolder")) {
                throw new NotExportableException("bad project description");
            }
            projectName = manifestTitle;
            projects.getSession().move(projects.getPath()+"/hippo:initialize", projects.getPath()+"/"+projectName);;
            projectNode = projects.getNode(projectName);
            projects.save();
        }
        void expand(ProjectElement projectElement, Node contentScratch) throws RepositoryException, NotExportableException {
            boolean userProject = false;
            for (NodeIterator iter = projectNode.getNodes(); iter.hasNext();) {
                Node node = iter.nextNode();
                if (node != null) {
                    if (node.isNodeType("hipposys:initializeitem")) {
                        throw new NotExportableException("unsupported initialize item definition");
                    }
                    if (node.isNodeType("hippo:initializeitem")) {
                        if (node.hasProperty("hippo:contentdelete")) {
                            if(userProject)
                                throw new NotExportableException("unsupported");
                        }
                        if (node.hasProperty("hippo:namespace")) {
                            if (node.hasProperty("hippo:nodetypes")) {
                                if(userProject)
                                    throw new NotExportableException("not supported for user projects");
                            } else if (node.hasProperty("hippo:nodetypesresource")) {
                                projectElement.elements.add(new NamespaceElement(node.getName(), node.getProperty("hippo:namespace").getString(), node.getProperty("hippo:nodetypesresource").getString()));
                            } else {
                                if(userProject)
                                    throw new NotExportableException("unsupported");
                            }
                        } else {
                            if (node.hasProperty("hippo:nodetypes") || node.hasProperty("hippo:nodetypesresource")) {
                                if(userProject)
                                    throw new NotExportableException("not supported for user projects");
                            }
                        }
                        if (node.hasProperty("hippo:contentresource")) {
                            if (!node.hasProperty("hippo:contentroot")) {
                                throw new NotExportableException("malformed");
                            }
                            /*
                            String absPath = node.getProperty("hippo:contentroot").getString();
                            String relPath = (absPath.startsWith("/") ? absPath.substring(1) : absPath);
                            Node content = (relPath.equals("") ? contentScratch : contentScratch.getNode(relPath));
                            projectElement.elements.add(new ContentElement(node.getName(), absPath, node.getProperty("hippo:contentresource").getString(), content));
                            */
                        } else if (node.hasProperty("hippo:content")) {
                            throw new NotExportableException("unsupprted");
                        }
                    } else {
                        throw new NotExportableException("unsupported");
                    }
                }
            }
        }
    }
}

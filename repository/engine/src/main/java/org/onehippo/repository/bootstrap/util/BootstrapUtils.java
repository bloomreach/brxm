/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipFile;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.jackrabbit.HippoCompactNodeTypeDefReader;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.util.FileContentResourceLoader;
import org.onehippo.repository.util.ZipFileContentResourceLoader;
import org.onehippo.repository.xml.ContentResourceLoader;
import org.onehippo.repository.xml.ImportResult;

import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

public class BootstrapUtils {

    private BootstrapUtils() {}

    public static int getNodeIndex(final Session session, final String nodePath) throws RepositoryException {
        final Node node = JcrUtils.getNodeIfExists(nodePath, session);
        if (node != null && node.getParent().getPrimaryNodeType().hasOrderableChildNodes()) {
            final NodeIterator nodes = node.getParent().getNodes();
            int index = 0;
            while (nodes.hasNext()) {
                if (nodes.nextNode().isSame(node)) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    public static void reorderNode(final Session session, final String nodePath, final int index) throws RepositoryException {
        final Node node = session.getNode(nodePath);
        final String srcChildRelPath = node.getName() + "[" + node.getIndex() + "]";
        final Node parent = node.getParent();
        final NodeIterator nodes = parent.getNodes();
        nodes.skip(index);
        if (nodes.hasNext()) {
            final Node destChild = nodes.nextNode();
            String destChildRelPath = destChild.getName() + "[" + destChild.getIndex() + "]";
            if (!srcChildRelPath.equals(destChildRelPath)) {
                parent.orderBefore(srcChildRelPath, destChildRelPath);
            }
        }
    }

    public static void removeNode(Session session, String absPath, boolean save) throws RepositoryException {
        if (!absPath.startsWith("/")) {
            throw new RepositoryException(String.format("Not an absolute path: %s", absPath));
        }
        if (session.nodeExists(absPath)) {
            final int offset = absPath.lastIndexOf('/');
            final String nodeName = absPath.substring(offset+1);
            final String parentPath = offset == 0 ? "/" : absPath.substring(0, offset);
            final Node parent = session.getNode(parentPath);
            if (parent.getNodes(nodeName).getSize() > 1) {
                throw new RepositoryException(String.format("Removing same name sibling is not supported"));
            } else {
                session.getNode(absPath).remove();
                if (save) {
                    session.save();
                }
            }
        }
    }

    public static ImportResult initializeNodecontent(Session session, String parentAbsPath, InputStream istream, URL location) {
        return initializeNodecontent(session, parentAbsPath, istream, location, false);
    }

    public static ImportResult initializeNodecontent(Session session, String parentAbsPath, InputStream istream, URL location, boolean pckg) {
        if (location != null) {
            log.debug("Initializing content from: {} to {}", location, parentAbsPath);
        } else {
            log.debug("Initializing content to {}", parentAbsPath);
        }
        File tempFile = null;
        ZipFile zipFile = null;
        InputStream esvIn = null;
        FileOutputStream out = null;
        try {
            if (session instanceof HippoSession) {
                HippoSession hippoSession = (HippoSession) session;
                int uuidBehaviour = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
                int referenceBehaviour = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
                if (pckg) {
                    tempFile = File.createTempFile("package", ".zip");
                    out = new FileOutputStream(tempFile);
                    IOUtils.copy(istream, out);
                    out.close();
                    out = null;
                    zipFile = new ZipFile(tempFile);
                    ContentResourceLoader contentResourceLoader = new ZipFileContentResourceLoader(zipFile);
                    esvIn = contentResourceLoader.getResourceAsStream("esv.xml");
                    return hippoSession.importEnhancedSystemViewXML(parentAbsPath, esvIn, uuidBehaviour, referenceBehaviour, contentResourceLoader);
                } else {
                    ContentResourceLoader contentResourceLoader = null;
                    if (location != null) {
                        int offset = location.getFile().indexOf(".jar!");
                        if (offset != -1) {
                            zipFile = new ZipFile(getBaseZipFileFromURL(location));
                            contentResourceLoader = new ZipFileContentResourceLoader(zipFile);
                        } else if (location.getProtocol().equals("file")) {
                            File sourceFile = new File(location.toURI());
                            contentResourceLoader = new FileContentResourceLoader(sourceFile.getParentFile());
                        }
                    }
                    return hippoSession.importEnhancedSystemViewXML(parentAbsPath, istream, uuidBehaviour, referenceBehaviour, contentResourceLoader);
                }
            } else {
                throw new IllegalStateException("Not a HippoSession");
            }
        } catch (IOException | RepositoryException | URISyntaxException e) {
            if (log.isDebugEnabled()) {
                log.error("Error initializing content for " + location + " in '" + parentAbsPath + "' : " + e, e);
            } else {
                log.error("Error initializing content for " + location + " in '" + parentAbsPath + "' : " + e);
            }
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(esvIn);
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception ignore) {
                }
            }
            FileUtils.deleteQuietly(tempFile);
        }
        return null;
    }

    public static void initializeNodetypes(Workspace workspace, InputStream cndStream, String cndName) throws RepositoryException {
        try {
            log.debug("Initializing nodetypes from: " + cndName);
            CompactNodeTypeDefReader<QNodeTypeDefinition,NamespaceMapping> cndReader =
                    new HippoCompactNodeTypeDefReader<>(new InputStreamReader(cndStream), cndName, workspace.getNamespaceRegistry(), new QDefinitionBuilderFactory());
            List<QNodeTypeDefinition> ntdList = cndReader.getNodeTypeDefinitions();
            NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
            NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

            for (QNodeTypeDefinition ntd : ntdList) {
                try {
                    ntreg.registerNodeType(ntd);
                    log.debug("Registered node type: " + ntd.getName().getLocalName());
                } catch (NamespaceException ex) {
                    log.error(ex.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), ex);
                } catch (InvalidNodeTypeDefException ex) {
                    if (ex.getMessage().endsWith("already exists")) {
                        try {
                            ntreg.reregisterNodeType(ntd);
                            log.debug("Replaced node type: " + ntd.getName().getLocalName());
                        } catch (NamespaceException e) {
                            log.error(e.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), e);
                        } catch (InvalidNodeTypeDefException e) {
                            log.info(e.getMessage() + ". In " + cndName + " for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), e);
                        } catch (RepositoryException e) {
                            if (!e.getMessage().equals("not yet implemented")) {
                                log.warn(e.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), e);
                            }
                        }
                    } else {
                        log.error(ex.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), ex);
                    }
                } catch (RepositoryException ex) {
                    if (!ex.getMessage().equals("not yet implemented")) {
                        log.warn(ex.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), ex);
                    }
                }
            }
        } catch (ParseException e) {
            throw new RepositoryException("Failed to parse cnd " + cndName, e);
        }
    }

    /**
     * Returns a {@link java.io.File} object which bases the input JAR / ZIP file URL.
     * <P>
     * For example, if the <code>url</code> represents "file:/a/b/c.jar!/d/e/f.xml", then
     * this method will return a File object representing "file:/a/b/c.jar" from the input.
     * </P>
     * @param url
     * @return
     * @throws URISyntaxException
     */
    public static File getBaseZipFileFromURL(final URL url) throws URISyntaxException {
        String file = url.getFile();
        int offset = file.indexOf(".jar!");

        if (offset == -1) {
            throw new IllegalArgumentException("Not a jar or zip url: " + url);
        }

        file = file.substring(0, offset + 4);

        if (!file.startsWith("file:")) {
            if (file.startsWith("/")) {
                file = "file://" + file;
            } else {
                file = "file:///" + file;
            }
        }

        return new File(URI.create(file));
    }

}

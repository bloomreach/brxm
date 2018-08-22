/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.upload;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * Helper class that is used to expand jar archives in the repository.
 * It only exists to work around a bug in JackRabbit where the zipped
 * stream is closed during decompression.  This occurs when JackRabbit
 * is approached over RMI.
 *
 * @deprecated This class is no longer used by Hippo CMS. It will be removed in a future major version.
 */
@Deprecated
public class JarExpander {


    /** The name of the node type required to expand the archive */
    public static final String TYPE_JARFILE = "rep:jarFile";

    /** The name of the child node taking the expanded archive */
    public static final String NODE_JARCONTENTS = "rep:jarContents";

    public static final String PROP_EXPAND_DATE = "rep:jarExpanded";

    private Node node;
    private Property prop;
    private String path;

    public JarExpander(Node node) throws RepositoryException {
        this.node = node;
        prop = node.getProperty("jcr:data");
        path = prop.getPath();
    }

    public void extract() throws RepositoryException {
        node.addMixin(TYPE_JARFILE);
        node.setProperty(PROP_EXPAND_DATE, Calendar.getInstance());

        if (node.hasNode(NODE_JARCONTENTS)) {
            node.getNode(NODE_JARCONTENTS).remove();
        }

        Node jarRoot = node.addNode(NODE_JARCONTENTS, "nt:folder");
        unpack(jarRoot);

        // update last modified date, there is a bug in the dynamic
        // class loader where the wrong comparison is made.  (i.e.
        // a jar IS NOT unpacked when it HAS changed, but IS unpacked
        // when it HAS NOT changed)
        node.setProperty("jcr:lastModified", Calendar.getInstance());
    }

    public String getPath() {
        return path;
    }

    public Property getProperty() {
        return prop;
    }

    public void unpack(Node jarRoot) throws RepositoryException {

        ZipInputStream zin = null;
        try {
            zin = new ZipInputStream(getProperty().getStream());
            ZipEntry entry = zin.getNextEntry();
            while (entry != null) {
                if (entry.isDirectory()) {
                    unpackFolder(jarRoot, entry.getName());
                } else {
                    unpackFile(jarRoot, entry, zin);
                }
                entry = zin.getNextEntry();
            }
        } catch (IOException ioe) {
            throw new RepositoryException("Problem reading JAR contents of " + getPath(), ioe);
        } finally {
            // close the JAR stream if open
            if (zin != null) {
                try {
                    zin.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private Node unpackFolder(Node root, String path) throws RepositoryException {

        // remove trailing slash
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // quick check if the folder already exists
        if (root.hasNode(path)) {
            return root.getNode(path);
        }

        // go down and create the path
        StringTokenizer tokener = new StringTokenizer(path, "/");
        while (tokener.hasMoreTokens()) {
            String label = tokener.nextToken();
            if (root.hasNode(label)) {
                root = root.getNode(label);
            } else {
                root = root.addNode(label, "nt:folder");
            }
        }

        // return the final node
        return root;
    }

    private void unpackFile(Node root, ZipEntry entry, InputStream ins) throws RepositoryException {
        int slash = entry.getName().lastIndexOf('/');
        String label = entry.getName().substring(slash + 1);
        Node parent = (slash <= 0) ? root : unpackFolder(root, entry.getName().substring(0, slash));

        // remove existing node (and all children by the way !!)
        if (parent.hasNode(label)) {
            parent.getNode(label).remove();
        }

        // prepare property values
        Calendar lastModified = Calendar.getInstance();
        lastModified.setTimeInMillis(entry.getTime());
        String mimeType = URLConnection.guessContentTypeFromName(label);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        // create entry nodes
        Node ntFile = parent.addNode(label, "nt:file");
        Node content = ntFile.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:mimeType", mimeType);
        content.setProperty("jcr:data", new Filter(ins));
        content.setProperty("jcr:lastModified", lastModified);
    }

    // Need to filter the stream, it is closed by the serialization mechanism
    private static class Filter extends FilterInputStream {
        public Filter(InputStream stream) {
            super(stream);
        }

        @Override
        public void close() throws IOException {
            // ignore
        }
    }
}

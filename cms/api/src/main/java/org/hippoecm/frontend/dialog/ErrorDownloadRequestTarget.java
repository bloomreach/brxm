/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.dialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Application;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.RepoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NodeInfo {

    private final boolean modified;
    private final int depth;
    private final int index;
    private final String name;
    private final String primaryType;
    private final String[] mixins;
    private final String uuid;

    NodeInfo(Node node, boolean modified) throws RepositoryException {
        this.depth = node.getDepth();
        this.name = node.getName();
        this.index = node.getIndex();
        this.modified = modified;
        if (node.isNodeType("mix:referenceable")) {
            this.uuid = node.getUUID();
        } else {
            this.uuid = null;
        }
        this.primaryType = node.getPrimaryNodeType().getName();
        if (node.hasProperty("jcr:mixinTypes")) {
            Value[] values = node.getProperty("jcr:mixinTypes").getValues();
            this.mixins = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                getMixins()[i] = values[i].getString();
            }
        } else {
            this.mixins = new String[0];
        }
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isModified() {
        return modified;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public String[] getMixins() {
        return mixins;
    }
}

enum Column {
    MODIFIED {

        @Override
        int getWidth(NodeInfo info) {
            return 1;
        }

        @Override
        public String render(NodeInfo info) {
            if (info.isModified()) {
                return "*";
            } else {
                return " ";
            }
        };
    },
    PATH {

        @Override
        public int getWidth(NodeInfo info) {
            return 4 * info.getDepth() + info.getName().length();
        }

        @Override
        public String render(NodeInfo info) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < info.getDepth(); i++) {
                sb.append("    ");
            }
            sb.append(info.getName());
            return sb.toString();
        }

    },
    UUID {
        @Override
        public int getWidth(NodeInfo info) {
            if (info.getUuid() != null) {
                return 36;
            } else {
                return 0;
            }
        }

        @Override
        public String render(NodeInfo info) {
            if (info.getUuid() != null) {
                return info.getUuid();
            } else {
                return "";
            }
        }
    },
    PRIMARY {
        @Override
        public int getWidth(NodeInfo info) {
            return info.getPrimaryType().length();
        }

        @Override
        public String render(NodeInfo info) {
            return info.getPrimaryType();
        }
    },
    MIXINS {

        @Override
        public int getWidth(NodeInfo info) {
            int length = 0;
            String[] mixins = info.getMixins();
            for (String mixin : mixins) {
                length += mixin.length();
            }
            if (mixins.length > 1) {
                length += (mixins.length - 1) * 2;
            }
            return length;
        }

        @Override
        public String render(NodeInfo info) {
            StringBuilder sb = new StringBuilder();
            String[] mixins = info.getMixins();
            for (int i = 0; i < mixins.length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(mixins[i]);
            }
            return sb.toString();
        }

    };

    abstract int getWidth(NodeInfo info);

    abstract String render(NodeInfo info);
}

public class ErrorDownloadRequestTarget implements IRequestTarget {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ErrorDownloadRequestTarget.class);

    static final String FILE_NAME = "hippo-cms-error.txt";

    private transient File tempFile;
    private transient FileInputStream fis;
    private Exception ex;

    ErrorDownloadRequestTarget(Exception ex) {
        this.ex = ex;
    }

    /**
     * @see org.apache.wicket.IRequestTarget#respond(org.apache.wicket.RequestCycle)
     */
    public void respond(RequestCycle requestCycle) {
        final Application app = Application.get();

        // Determine encoding
        final String encoding = app.getRequestCycleSettings().getResponseRequestEncoding();

        // Set content type based on markup type for page
        final WebResponse response = (WebResponse) requestCycle.getResponse();
        response.setCharacterEncoding(encoding);
        response.setContentType("text/xml; charset=" + encoding);

        // Make sure it is not cached by a client
        response.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
        response.setHeader("Cache-Control", "no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setLastModifiedTime(Time.now());

        // set filename
        response.setAttachmentHeader(FILE_NAME);

        UserSession session = UserSession.get();

        try {
            tempFile = File.createTempFile("error-" + Time.now().toString() + "-", ".xml");
            FileOutputStream fos = new FileOutputStream(tempFile);
            try {
                PrintStream ps = new PrintStream(fos);

                ps.println("Hippo CMS Error report");
                ps.println("========================================");

                ps.print("version : ");
                ps.print(getCMSVersion());
                ps.println();

                ps.print("time    : ");
                ps.print(Time.now().toString());
                ps.println();

                javax.jcr.Session jcrSession = session.getJcrSession();
                if (jcrSession != null) {
                    ps.print("user    : ");
                    ps.print(session.getJcrSession().getUserID());
                    ps.println();
                }

                HttpServletRequest hsr = ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest();
                ps.print("server  : ");
                ps.print(hsr.getServerName());
                ps.println();

                String node = System.getProperty("org.apache.jackrabbit.core.cluster.node_id");
                if (node != null) {
                    ps.print("node    : ");
                    ps.print(hsr.getServerName());
                    ps.println();
                }

                ps.print("java    : ");
                ps.print(System.getProperty("java.version"));
                ps.println();

                ps.println();

                ps.println("error   :");
                ps.print(ex.getClass().getName());
                ps.print(": ");
                ps.print(ex.getLocalizedMessage());
                ps.println();

                ex.printStackTrace(ps);
                ps.println();

                if (jcrSession != null) {
                    ps.println("session :");

                    Map<NodePath, NodeInfo> modificationTree = getModificationTree(jcrSession);
                    int widths[] = new int[Column.values().length];
                    for (Map.Entry<NodePath, NodeInfo> entry : modificationTree.entrySet()) {
                        NodeInfo info = entry.getValue();
                        for (Column col : Column.values()) {
                            int width = col.getWidth(info);
                            if (width > widths[col.ordinal()]) {
                                widths[col.ordinal()] = width;
                            }
                        }
                    }
                    int row = 0;
                    for (Map.Entry<NodePath, NodeInfo> entry : modificationTree.entrySet()) {
                        NodeInfo info = entry.getValue();
                        if (row % 5 == 0 && row != 0) {
                            ps.println();
                        }
                        ps.print("  ");
                        for (Column col : Column.values()) {
                            String val = col.render(info);
                            ps.print(val);
                            int fill = widths[col.ordinal()] - val.length();
                            for (int i = 0; i < fill; i++) {
                                ps.print(' ');
                            }
                            ps.print("   ");
                        }
                        ps.println();
                        row++;
                    }
                }
                ps.println();

                ps.println("========================================");

            } finally {
                fos.close();
            }
            fis = new FileInputStream(tempFile);
            response.write(fis);
        } catch (FileNotFoundException e) {
            log.error("Tempfile missing during export", e);
        } catch (IOException e) {
            log.error("IOException during export", e);
        }
    }

    private Map<NodePath, NodeInfo> getModificationTree(javax.jcr.Session jcrSession) {
        HippoSession hs = (HippoSession) jcrSession;
        Map<NodePath, NodeInfo> toPrint = new TreeMap<NodePath, NodeInfo>();
        try {
            NodeIterator iter = hs.pendingChanges();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                Node ancestor = node;
                NodePath path = new NodePath(ancestor.getPath());
                while (!toPrint.containsKey(path)) {
                    toPrint.put(path, new NodeInfo(ancestor, false));
                    if (ancestor.getDepth() == 0) {
                        break;
                    }
                    ancestor = ancestor.getParent();
                    path = new NodePath(ancestor.getPath());
                }
                toPrint.put(new NodePath(node.getPath()), new NodeInfo(node, true));
            }
        } catch (RepositoryException e) {
            log.error("Repository exception when analysing session", e);
        } catch (Throwable t) {
            log.error("Runtime exception when analysing session", t);
        }
        return toPrint;
    }

    /**
     * @see org.apache.wicket.IRequestTarget#detach(org.apache.wicket.RequestCycle)
     */
    public void detach(RequestCycle requestCycle) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                // ignore
            }
        }
        if (tempFile != null) {
            tempFile.delete();
        }
    }

    private String getCMSVersion() {
        StringBuilder sb = new StringBuilder();
        InputStream istream = null;
        try {
            try {
                // try to get the version from the frontend-engine manifest
                URL url = RepoUtils.getManifestURL(Home.class);
                if (url != null) {
                    istream = url.openStream();
                }
            } catch (IOException ignore) {
            }
            if (istream == null) {
                ServletContext servletContext = ((WebApplication) Application.get()).getServletContext();
                istream = servletContext.getResourceAsStream("META-INF/MANIFEST.MF");
                if (istream == null) {
                    File manifestFile = new File(servletContext.getRealPath("/"), "META-INF/MANIFEST.MF");
                    if (manifestFile.exists()) {
                        istream = new FileInputStream(manifestFile);
                    }
                }
            }
            if (istream != null) {
                Manifest manifest = new Manifest(istream);
                Attributes atts = manifest.getMainAttributes();
                if (atts.getValue("Implementation-Version") != null) {
                    sb.append(atts.getValue("Implementation-Version"));
                }
                if (atts.getValue("Implementation-Build") != null) {
                    sb.append(" build ");
                    sb.append(atts.getValue("Implementation-Build"));
                }
            }
        } catch (IOException ex) {
            // deliberate ignore
        }
        if (sb.length() == 0) {
            return "unknown";
        } else {
            return sb.toString();
        }
    }

    public static class NodePath implements Comparable<NodePath> {

        private String[] names;
        private Integer[] indices;
        private String string;

        public NodePath(String path) {
            this.string = path;

            String[] elements = path.split("/");
            names = new String[elements.length];
            indices = new Integer[elements.length];
            for (int i = 0; i < elements.length; i++) {
                String element = elements[i];
                if (element.contains("[")) {
                    names[i] = element.substring(0, element.indexOf('['));
                    indices[i] = Integer.parseInt(element.substring(element.indexOf('[') + 1, element.indexOf(']')));
                } else {
                    names[i] = element;
                    indices[i] = 0;
                }
            }
        }

        public int compareTo(NodePath that) {
            NodePath one = this;
            NodePath two = that;
            boolean reverse = false;
            if (that.names.length < names.length) {
                one = that;
                two = this;
                reverse = true;
            }

            int result = 0;
            for (int i = 0; i < one.names.length; i++) {
                result = one.names[i].compareTo(two.names[i]);
                if (result != 0) {
                    break;
                }
                result = one.indices[i].compareTo(two.indices[i]);
                if (result != 0) {
                    break;
                }
            }
            if (result == 0) {
                if (two.names.length > one.names.length) {
                    result = -1;
                }
            }
            if (reverse) {
                return -result;
            }
            return result;
        }

        // warning: part of interface
        @Override
        public String toString() {
            return string;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof NodePath) {
                NodePath that = (NodePath) obj;
                return that.string.equals(this.string);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return string.hashCode() ^ 137;
        }
    }

}
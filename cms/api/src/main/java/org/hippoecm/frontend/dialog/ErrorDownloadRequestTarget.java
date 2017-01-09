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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Application;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
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

public class ErrorDownloadRequestTarget implements IRequestHandler {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ErrorDownloadRequestTarget.class);

    static final String FILE_NAME = "hippo-cms-error.txt";

    private Exception ex;

    ErrorDownloadRequestTarget(Exception ex) {
        this.ex = ex;
    }

    /**
     * @see org.apache.wicket.request.IRequestHandler#respond(org.apache.wicket.request.IRequestCycle)
     */
    public void respond(IRequestCycle requestCycle) {
        final Application app = Application.get();

        // Determine encoding
        final String encoding = app.getRequestCycleSettings().getResponseRequestEncoding();

        // Set content type based on markup type for page
        final WebResponse response = (WebResponse) requestCycle.getResponse();
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

            ByteArrayOutputStream fos = new ByteArrayOutputStream();
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

                HttpServletRequest hsr = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
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
            response.write(fos.toByteArray());
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
     * @see org.apache.wicket.request.IRequestHandler#detach(org.apache.wicket.request.IRequestCycle)
     */
    public void detach(IRequestCycle requestCycle) {
    }

    private String getCMSVersion() {
        final String implVersion = Home.class.getPackage().getImplementationVersion();
        if (implVersion != null) {
            return implVersion;
        }
        return "unknown";
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
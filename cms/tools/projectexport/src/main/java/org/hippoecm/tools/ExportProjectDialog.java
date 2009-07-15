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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.wicket1985.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportProjectDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ExportProjectDialog.class);

    private transient ProjectExport export;

    private String projectName;
    
    public ExportProjectDialog() {
        this.setOkVisible(false);
        this.setCancelVisible(false);
        try {
            export = new ProjectExport(((UserSession) org.apache.wicket.Session.get()).getJcrSession());
        } catch(RepositoryException ex) {
            log.error("Everything is broken", ex);
        } catch(IOException ex) {
            log.error("Things are broken", ex);
        } catch(NotExportableException ex) {
            log.error("Some things are broken", ex);
        }
        add(new ExportProjectTree("tree", new ExportProjectTreeModel(export)));
        add(new DownloadLink("download", new Model("download")) {
            protected String getFilename() {
                return projectName + ".zip";
            }
            protected InputStream getContent() {
                try {
                    export.selectProject(projectName);
                    final PipedOutputStream ostream = new PipedOutputStream();
                    final PipedInputStream pstream = new PipedInputStream(ostream);
                    Thread thread = new Thread() {
                        public void run() {
                            try {
                                try {
                                    export.exportProject(ostream);
                                } catch (RepositoryException ex) {
                                    pstream.close();
                                } catch (IOException ex) {
                                    pstream.close();
                                } catch (NotExportableException ex) {
                                    pstream.close();
                                }
                            } catch (IOException ex) {
                                // deliberate ignore
                            }
                        }
                    };
                    thread.run();
                    return pstream;
                } catch(RepositoryException ex) {
                    log.error("failed to export project ", ex);
                    return null;
                } catch(IOException ex) {
                    log.error("failed to export project ", ex);
                    return null;
                } catch(NotExportableException ex) {
                    log.error("failed to export project ", ex);
                    return null;
                }
            }
        });
    }

    public IModel getTitle() {
        return new Model("Export Project");
    }
}

class ExportProjectTree extends Tree implements Serializable {
    public ExportProjectTree(String id, ExportProjectTreeModel model) {
        super(id, model);
        setRootLess(true);
    }
}

class ExportProjectTreeNode implements TreeNode, Serializable {
    private String name;
    ExportProjectTreeNode parent;
    List<ExportProjectTreeNode> children;

    ExportProjectTreeNode(ProjectExport export) {
        name = "Projects";
        System.err.println("BERRY "+name);
        parent = null;
        List<Element> elements = export.getElements(null);
        if(elements != null) {
            children = new LinkedList<ExportProjectTreeNode>();
            for(Element child : elements) {
                children.add(new ExportProjectTreeNode(export, this, child));
            }
        } else {
            children = null;
        }
    }
    private ExportProjectTreeNode(ProjectExport export, ExportProjectTreeNode parent, Element element) {
        name = element.getFullName();
        System.err.println("BERRY "+name);
        this.parent = parent;
        List<Element> elements = export.getElements(element);
        if(elements != null) {
            children = new LinkedList<ExportProjectTreeNode>();
            for(Element child : elements) {
                children.add(new ExportProjectTreeNode(export, this, child));
            }
        } else {
            children = null;
        }
    }

    /**
     * Returns the child <code>TreeNode</code> at index 
     * <code>childIndex</code>.
     */
    public TreeNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    /**
     * Returns the number of children <code>TreeNode</code>s the receiver
     * contains.
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Returns the parent <code>TreeNode</code> of the receiver.
     */
    public TreeNode getParent() {
        return parent;
    }

    /**
     * Returns the index of <code>node</code> in the receivers children.
     * If the receiver does not contain <code>node</code>, -1 will be
     * returned.
     */
    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return children != null;
    }

    /**
     * Returns true if the receiver is a leaf.
     */
    public boolean isLeaf() {
        return !getAllowsChildren() || children.size() == 0;
    }

    /**
     * Returns the children of the receiver as an <code>Enumeration</code>.
     */
    public Enumeration<TreeNode> children() {
        final Iterator<ExportProjectTreeNode> iter = children.iterator();
        return new Enumeration<TreeNode>() {
            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            public TreeNode nextElement() {
                return iter.next();
            }
        };
    }
    
    public String toString() {
        return name;
    }
}

class ExportProjectTreeModel extends DefaultTreeModel implements TreeModel, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    ExportProjectTreeModel(ProjectExport export) {
        super(new ExportProjectTreeNode(export));
        try {
            export.selectProject("Gallery Addon");
            export.exportProject(new FileOutputStream("test.zip"));
        } catch(NotExportableException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(IOException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(RepositoryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    public void detach() {
    }
}

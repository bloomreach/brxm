/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.model.tree;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.swing.tree.DefaultTreeModel;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.JcrEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTreeModel extends DefaultTreeModel implements IObservable, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(JcrTreeModel.class);

    private IObservationContext observationContext;
    private JcrEventListener listener;
    private AbstractTreeNode root;

    public JcrTreeModel(AbstractTreeNode rootModel) {
        super(rootModel);

        root = rootModel;
    }

    public AbstractTreeNode lookup(JcrNodeModel nodeModel) {
        AbstractTreeNode node = root;
        if (nodeModel != null) {
            String basePath = root.getNodeModel().getItemModel().getPath();
            String path = nodeModel.getItemModel().getPath();
            if (path.startsWith(basePath)) {
                String[] elements = StringUtils.split(path.substring(basePath.length()), '/');
                try {
                    for (String element : elements) {
                        AbstractTreeNode child = node.getChild(element);
                        if (child != null) {
                            node = child;
                        } else {
                            break;
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error("Unable to find node in tree", ex.getMessage());
                }
            }
        }
        return node;
    }

    public void detach() {
        root.detach();
    }

    /*
    @Override
    public void addTreeModelListener(TreeModelListener l) {
        
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        // TODO Auto-generated method stub
        super.removeTreeModelListener(l);
    }
    */

    public void setObservationContext(IObservationContext context) {
        this.observationContext = context;
    }

    public void startObservation() {
        listener = new JcrEventListener(observationContext, Event.NODE_ADDED | Event.NODE_REMOVED, root
                .getNodeModel().getItemModel().getPath(), true, null, null);
        listener.start();
    }

    public void stopObservation() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
    }
}

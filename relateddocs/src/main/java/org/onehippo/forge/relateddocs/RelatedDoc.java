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
package org.onehippo.forge.relateddocs;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RelatedDoc contains the JcrNodeModel of the document that is related and its score, based on the configured
 * implementations of the IRelatedDocsProvider
 */
public class RelatedDoc implements Comparable<RelatedDoc>, IDetachable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(RelatedDoc.class);
    private JcrNodeModel nodeModel;
    private String uuid;
    private double score = 1.0;

    public RelatedDoc() {

    }

    /**
     * Constructor used when the UUID of the JcrNodeModel and Session are available. Will look up the JcrNodeModel based
     * on the UUID and JcrNodeModel Sets score to 0.0
     *
     * @param session
     * @param uuid
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public RelatedDoc(Session session, String uuid) throws ItemNotFoundException, RepositoryException {
        this(session, uuid, 0.0);
    }

    /**
     * Constructor used when the UUID of the JcrNodeModel and Session are available. Will look up the JcrNodeModel based
     * on the UUID and JcrNodeModel and sets the score
     *
     * @param session
     * @param uuid
     * @param score
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public RelatedDoc(Session session, String uuid, double score) throws ItemNotFoundException, RepositoryException {
        this.uuid = uuid;
        nodeModel = new JcrNodeModel(session.getNodeByIdentifier(uuid));
        setScore(score);
    }

    /**
     * Constructor used when the JcrNodeModel is known. Sets score to 0.0
     *
     * @param nodeModel
     */
    public RelatedDoc(JcrNodeModel nodeModel) {
        this(nodeModel, 0.0);
    }

    /**
     * Constructor used when the JcrNodeModel is known.
     *
     * @param nodeModel
     * @param score
     */
    public RelatedDoc(JcrNodeModel nodeModel, double score) {
        setNodeModel(nodeModel);
        setScore(score);
    }

    public JcrNodeModel getNodeModel() {
        return nodeModel;
    }

    public void setNodeModel(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
        try {
            this.uuid = nodeModel.getNode().getIdentifier();
        } catch (UnsupportedRepositoryOperationException e) {
            this.uuid = "";
            log.error("Error retreiving UUID of a Node", e);
        } catch (RepositoryException e) {
            this.uuid = "";
            log.error("Error retreiving UUID of a Node", e);
        }
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void addScore(double score) {
        this.score += score;
    }

    /**
     * @return uuid of JCR Node for the JcrNodeModel of the RelatedDoc
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @return (user friendly) name of the Node of the JcrNodeModel of the RelatedDoc
     * @throws RepositoryException
     */
    public String getName() {
        return new NodeTranslator(nodeModel).getNodeName().getObject();
    }

    public String getPath() {
        try {
            return nodeModel.getNode().getPath().replaceFirst("/content/","");
        } catch (RepositoryException e) {
            log.error("Could not retrieve path");
            return null;
        }
    }

    public int compareTo(RelatedDoc o) {
        if (o.getUuid().equals(getUuid())) {
            return 0;
        } else if (o.getScore() > score) {
            return 1;
        } else if (o.getScore() < score) {
            return -1;
        } else {
            int compare;
            try {
                compare = getName().compareToIgnoreCase(o.getName());
            } catch (NullPointerException e) {
                String thisPath = getNodeModel().getItemModel().getPath();
                String otherPath = o.getNodeModel().getItemModel().getPath();
                compare = thisPath.compareTo(otherPath);
            }
            return compare;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RelatedDoc) {
            RelatedDoc other = (RelatedDoc) obj;
            return this.getUuid().equals(other.getUuid());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

    public void detach() {
        nodeModel.detach();
    }

    /**
     * This method checks whether the document this {@link RelatedDoc} links to, still exists in the repository and is
     * not deleted.
     *
     * @return {@code true} if the {@link RelatedDoc} exists, {@link false} otherwise
     */
    public boolean exists() {
        try {
            Node node = nodeModel.getNode();
            if (node == null) {
                node = UserSession.get().getJcrSession().getNodeByIdentifier(uuid);
            }

            return node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName()) &&
                   !node.getNode(node.getName()).isNodeType(HippoNodeType.NT_DELETED);

        } catch (ItemNotFoundException ex) {
            return false;
        } catch (RepositoryException ex) {
            // Give this document one more chance as the exception might not be related
            // to the fact that the related document does not exist anymore.
            return true;
        }
    }
}

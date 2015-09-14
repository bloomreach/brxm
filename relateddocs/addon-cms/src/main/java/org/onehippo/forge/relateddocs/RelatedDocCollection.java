/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a collection of RelatedDocs. The collection contains just one of every RelatedDoc. When adding existing
 * RelatedDocs the score off the new RelatedDoc is added to the existing one. You can also add collections to each
 * other.
 * <p/>
 * A Collection also has a multiplier. It can be used to make collections more important than other collections.
 * <p/>
 * based on org.hippoecm.frontend.plugins.tagging.TagCollection
 */
public class RelatedDocCollection implements Cloneable, IDataProvider<RelatedDoc>, IDetachable, Iterable<RelatedDoc> {

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(RelatedDocCollection.class);
    private JcrNodeModel nodeModel;
    private SortedMap<RelatedDoc, RelatedDoc> related = new TreeMap<RelatedDoc, RelatedDoc>();

    /**
     * Constructs a RelatedDocCollection that is not written to the repository (e.g. suggestionlist)
     */
    public RelatedDocCollection() {
        this.nodeModel = null;
    }

    /**
     * Constructs a RelatedDocCollection that is read from and written to the repository
     *
     * @param nodeModel the JCR node model of the relateddocs:docs child node
     */
    public RelatedDocCollection(JcrNodeModel nodeModel) {

        this.nodeModel = nodeModel;

        loadDocs();

    }

    /**
     * Loads the related documents.
     */
    private void loadDocs() {
        if (nodeModel == null) {
            return;
        }

        try {
            Node node = nodeModel.getNode();
            if (node == null) {
                return;
            }
            //If there are no children just return
            if (!node.hasNodes()) {
                return;
            }

            double i;
            if (size() > 0) {
                i = related.lastKey().getScore() / 2;
            } else {
                i = 1.0;
            }

            NodeIterator docs = node.getNodes();
            final javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            while (docs.hasNext()) {
                final String uuid = docs.nextNode().getProperty("hippo:docbase").getString();
                final RelatedDoc relatedDoc = createIfExists(session, uuid, i);
                if (relatedDoc != null) {
                    related.put(relatedDoc, relatedDoc);
                    i /= 2;
                }
            }
        } catch (RepositoryException e) {
            log.error("Repository error parsing the PropertyModel", e);
        }

    }

    public void add(RelatedDoc r) {
        String uuid = r.getUuid();
        if (log.isDebugEnabled()) {
            log.debug("Trying to add RelatedDoc " + r + " with UUID " + uuid);
        }
        if (StringUtils.isBlank(uuid)) {
            if (log.isWarnEnabled()) {
                log.warn("Empty UUID returned, will not add this related doc");
            }
            return;
        }
        boolean exists = related.containsKey(r);
        if (exists) {
            RelatedDoc relatedDoc = related.get(r);
            relatedDoc.addScore(r.getScore());
        } else {
            related.put(r, r);
        }

        if (nodeModel == null || nodeModel.getNode() == null) {
            return;
        }

        try {
            Node docFacetSelect = nodeModel.getNode().addNode(RelatedDocsNodeType.RELATEDDOCS_RELDOC, HippoNodeType.NT_FACETSELECT);
            docFacetSelect.setProperty(HippoNodeType.HIPPO_DOCBASE, r.getUuid());
            docFacetSelect.setProperty(HippoNodeType.HIPPO_VALUES, ArrayUtils.EMPTY_STRING_ARRAY);
            docFacetSelect.setProperty(HippoNodeType.HIPPO_FACETS, ArrayUtils.EMPTY_STRING_ARRAY);
            docFacetSelect.setProperty(HippoNodeType.HIPPO_MODES, ArrayUtils.EMPTY_STRING_ARRAY);
        } catch (ValueFormatException e) {
            log.error("Failed trying to save related document '" + r + "'.", e);
        } catch (VersionException e) {
            log.error("Versioning error.", e);
        } catch (LockException e) {
            log.error("Locking error.", e);
        } catch (ConstraintViolationException e) {
            log.error("Constraint violation", e);
        } catch (RepositoryException e) {
            log.error("Repository error.", e);
        }
    }

    public void addAll(RelatedDocCollection col) {
        RelatedDocCollection collection = (RelatedDocCollection) col.clone();
        collection.normalizeScores();
        for (RelatedDoc r : collection.related.keySet()) {
            add(r);
        }
    }

    public void remove(RelatedDoc r) {
        final String uuid = r.getUuid();

        if (log.isDebugEnabled()) {
            log.debug("Trying to remove RelatedDoc {} with UUID {}", r, uuid);
        }
        if (StringUtils.isEmpty(uuid)) {
            log.warn("Cannot remove related document: its UUID is empty ('{}')'", uuid);
            return;
        }

        if (related.containsKey(r)) {
            related.remove(r);
            try {
                NodeIterator it = nodeModel.getNode().getNodes(RelatedDocsNodeType.RELATEDDOCS_RELDOC);
                while (it.hasNext()) {
                    Node n = it.nextNode();
                    if (n.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                        Property docbase = n.getProperty(HippoNodeType.HIPPO_DOCBASE);
                        if (docbase.getString().equals(uuid)) {
                            n.remove();
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error while removing related document with UUID '" + uuid + '\'', e);
            }
        }
    }

    public RelatedDoc lastKey() {
        return related.lastKey();
    }

    @Override
    protected RelatedDocCollection clone() {
        final RelatedDocCollection clone;
        try {
            clone = (RelatedDocCollection) super.clone();
            clone.related = new TreeMap<RelatedDoc, RelatedDoc>(clone.related);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (RelatedDoc relatedDoc : related.keySet()) {
            builder.append(relatedDoc.getUuid());
            builder.append(", ");
        }
        return builder.toString();
    }

    /**
     * Iterate over relatedDocs and normalize to a score between 0 and 100
     * TODO: should this manipulate the current collection or return a new one???
     * @return this object (for chaining method invocations)
     */
    public RelatedDocCollection normalizeScores() {
        double highest = -100.0;
        double lowest = -100.0;
        // detect highest and lowest score (needed to calculate the factor for
        // normalization)
        for (RelatedDoc relatedDoc : related.keySet()) {
            // set to values of first hit
            if (highest == -100.0) {
                highest = relatedDoc.getScore();
            }
            if (lowest == -100.0) {
                lowest = relatedDoc.getScore();
            }
            // check if this hit exceeds the values
            if (relatedDoc.getScore() > highest) {
                highest = relatedDoc.getScore();
            }
            if (relatedDoc.getScore() < lowest) {
                lowest = relatedDoc.getScore();
            }
        }
        double factor = (highest) / 100;
        for (RelatedDoc relatedDoc : related.keySet()) {
            relatedDoc.setScore((relatedDoc.getScore()) / factor);
        }
        return this; // for chaining
    }

    @Override
    public Iterator<RelatedDoc> iterator(long first, long count) {
        Iterator<RelatedDoc> iter = related.keySet().iterator();
        while (first > 0 && iter.hasNext()) {
            iter.next();
            first--;
        }
        return iter;
    }

    @Override
    public IModel<RelatedDoc> model(RelatedDoc doc) {
        return new Model<RelatedDoc>(doc);
    }

    @Override
    public long size() {
        return related.size();
    }

    @Override
    public void detach() {
        nodeModel.detach();
        for (RelatedDoc doc : related.keySet()) {
            doc.detach();
        }
    }

    @Override
    public Iterator<RelatedDoc> iterator() {
        return related.keySet().iterator();
    }

    /**
     * A private utility method to check if the referenced related document exists, if yes it constructs a new {@link
     * RelatedDoc} instance, otherwise returns null
     */
    private RelatedDoc createIfExists(final javax.jcr.Session session, final String uuid, final double score)
            throws RepositoryException {

        try {
            return new RelatedDoc(session, uuid, score);
        } catch (ItemNotFoundException ex) {
            if (log.isDebugEnabled()) {
                log.warn("Could not find item with uuid '" + uuid + "'", ex);
            } else {
                log.warn("Could not find item with uuid '{}'. {}", uuid, ex);
            }
        }
        return null;
    }

}

/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.decorating.DecoratorFactory;
import org.hippoecm.repository.deriveddata.DerivedDataEngine;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDecorator extends org.hippoecm.repository.decorating.NodeDecorator implements HippoNode {

    private static final Logger log = LoggerFactory.getLogger(NodeDecorator.class);

    protected NodeDecorator(DecoratorFactory factory, Session session, Node node) {
        super(factory, session, node);
    }

    public Node getCanonicalNode() throws RepositoryException {
        // Note that HREPTWO-2127 is still unresolved, even though the
        // previous implementation did have problems with it, but the current
        // implementation hasn't.  The point is that if you try to perform a
        // hasPRoperty you do not have the same view as with getProperty,
        // which is wrong.
        Node canonical = ((SessionDecorator)getSession()).getCanonicalNode(node);
        if(canonical != null) {
            return factory.getNodeDecorator(session, canonical);
        } else {
            return null;
        }
    }

    @Override
    public boolean isVirtual() throws RepositoryException {
        return getIdentifier().startsWith("cafeface");
    }

    @Override
    public boolean recomputeDerivedData() throws RepositoryException {
        if(item.isNode()) {
            return ((SessionDecorator)getSession()).computeDerivedData((Node) item);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void save() throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException, RepositoryException {
        if(item.isNode()) {
            ((SessionDecorator)getSession()).postSave((Node)item);
        }
        super.save();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() throws VersionException, LockException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            if(isNode()) {
                DerivedDataEngine.removal(this);
            }
            super.remove();
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * internal function to access the display name for a normal, Version or VersionHistory node.
     * @param node the <em>underlying</em> node
     * @return a symbolic name of the node
     */
    static String getDisplayName(Node node) throws RepositoryException {
        //if (node.hasProperty(HippoNodeType.HIPPO_UUID) && node.hasProperty(HippoNodeType.HIPPO_SEARCH)) {
        if (node.hasProperty(HippoNodeType.HIPPO_SEARCH)) {

            // just return the resultset
            if (node.getName().equals(HippoNodeType.HIPPO_RESULTSET)) {
                return HippoNodeType.HIPPO_RESULTSET;
            }

            // the last search is the current one
            Value[] searches = node.getProperty(HippoNodeType.HIPPO_SEARCH).getValues();
            if (searches.length == 0) {
                return node.getName();
            }
            String search = searches[searches.length-1].getString();

            // check for search seperator
            if (search.indexOf("#") == -1) {
                return node.getName();
            }

            // check for sql parameter '?'
            String xpath = search.substring(search.indexOf("#")+1);
            if (xpath.indexOf('?') == -1) {
                return node.getName();
            }

            // construct query
            xpath = xpath.substring(0,xpath.indexOf('?')) + node.getName() + xpath.substring(xpath.indexOf('?')+1);

            Query query = node.getSession().getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);

            // execute
            QueryResult result = query.execute();
            RowIterator iter = result.getRows();
            if (iter.hasNext()) {
                return iter.nextRow().getValues()[0].getString();
            } else {
                return node.getName();
            }
        } else {
            return node.getName();
        }
    }

    /**
     * @inheritDoc
     */
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            return super.checkin();
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        if(!isCheckedOut()) {
            try {
                ((SessionDecorator)getSession()).postMountEnabled(false);
                ((SessionDecorator)getSession()).postRefreshEnabled(false);
                super.checkout();
            } finally {
                ((SessionDecorator)getSession()).postMountEnabled(true);
                ((SessionDecorator)getSession()).postRefreshEnabled(true);
            }
        }
    }

    /**
     * @inheritDoc
     */
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
                                                     ConstraintViolationException, LockException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.removeMixin(mixinName);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void orderBefore(String srcChildRelPath, String destChildRelPath)
            throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException,
            ItemNotFoundException, LockException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.orderBefore(srcChildRelPath, destChildRelPath);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException,
            AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            return super.merge(srcWorkspace, bestEffort);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.restore(versionName, removeExisting);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.restore(version, removeExisting);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException,
            ItemExistsException, VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.restore(version, relPath, removeExisting);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException,
            ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException,
            RepositoryException {
        try {
            ((SessionDecorator)getSession()).postMountEnabled(false);
            super.restoreByLabel(versionLabel, removeExisting);
        } finally {
            ((SessionDecorator)getSession()).postMountEnabled(true);
        }
    }

    /**
     * @inheritDoc
     */
    public String getLocalizedName() throws RepositoryException {
        return getLocalizedName(null);
    }
    /**
     * @inheritDoc
     */
    public String getLocalizedName(Localized localized) throws RepositoryException {
        Node node = this;
        if (!node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
            if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                Node handle = node.getParent();
                if (handle.isNodeType(HippoNodeType.NT_HANDLE) && handle.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                    node = handle;
                } else {
                    return getName();
                }
            } else {
                return getName();
            }
        }
        if(localized == null) {
            localized = getLocalized(null);
            if (localized == null) {
                localized = Localized.getInstance();
            }
        }
        Node bestCandidateNode = null;
        Localized bestCandidate = null;
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            Node currentCandidateNode;
            try {
                currentCandidateNode = node.getNode(HippoNodeType.HIPPO_TRANSLATION + "[" + i + "]");
            } catch (PathNotFoundException e) {
                break;
            }
            Localized currentCandidate = Localized.getInstance(currentCandidateNode);
            Localized resultCandidate = localized.matches(bestCandidate, currentCandidate);
            if (resultCandidate == currentCandidate) {
                bestCandidate = currentCandidate;
                bestCandidateNode = currentCandidateNode;
            }
        }
        if (bestCandidateNode != null) {
            String localizedName = JcrUtils.getStringProperty(bestCandidateNode, HippoNodeType.HIPPO_MESSAGE, null);
            if (localizedName != null) {
                return localizedName;
            }
        }
        return getName();
    }

    @Override
    public Map<Localized, String> getLocalizedNames() throws RepositoryException {
        final Node node = getTranslatedNodeOrNull();
        if (node == null) {
            return Collections.emptyMap();
        }
        return getLocalizedNames(node);
    }

    private Node getTranslatedNodeOrNull() throws RepositoryException {
        Node node = this;
        if (!node.isNodeType(HippoNodeType.NT_TRANSLATED) && node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            final Node handle = node.getParent();
            if (handle.isNodeType(HippoNodeType.NT_HANDLE) && handle.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                node = handle;
            }
        }
        return node;
    }

    private Map<Localized, String> getLocalizedNames(final Node node) throws RepositoryException {
        final Map<Localized, String> localizedNames = new LinkedHashMap<Localized, String>();

        final NodeIterator nodeIterator = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
        while (nodeIterator.hasNext()) {
            final Node translationNode = nodeIterator.nextNode();
            final String language = translationNode.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();

            try {
                final Localized localized = getLocalizedForLanguage(language);
                final String message = translationNode.getProperty(HippoNodeType.HIPPO_MESSAGE).getString();
                localizedNames.put(localized, message);
            } catch (IllegalArgumentException e) {
                log.info("Ignoring localized name for language '{}'", language, e);
            }
        }
        return localizedNames;
    }

    private Localized getLocalizedForLanguage(final String language) throws IllegalArgumentException {
        if (StringUtils.isBlank(language)) {
            return Localized.getInstance();
        }
        final Locale locale = LocaleUtils.toLocale(language);
        return Localized.getInstance(locale);
    }

    public Localized getLocalized(Locale locale) throws RepositoryException {
        if (locale != null) {
            return Localized.getInstance(locale);
        }
        return null;
    }
}

/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.repository.standardworkflow;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.core.version.InternalVersionHistoryImpl;
import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;
import org.apache.jackrabbit.core.version.VersionHistoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

interface Tuple extends Serializable {
    public abstract boolean resolved();
}

class StringTuple implements Tuple {
    private String item;

    public StringTuple(String item) {
        this.item = item;
    }

    public String getItem() {
        return item;
    }

    public boolean equals(Object o) {
        if (o instanceof StringTuple) {
            StringTuple other = (StringTuple)o;
            if (item != null && other.item != null && !item.equals(other.item)) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean resolved() {
        return item != null;
    }
}

class StringPairTuple implements Tuple {
    private String source;
    private String target;

    public StringPairTuple(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public boolean equals(Object o) {
        if (o instanceof StringPairTuple) {
            StringPairTuple other = (StringPairTuple)o;
            if (source != null && other.source != null && !source.equals(other.source)) {
                return false;
            }
            if (target != null && other.target != null && !target.equals(other.target)) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean resolved() {
        return source != null && target != null;
    }
}

interface TupleSet<T extends Tuple> {
    public void put(T tuple);

    public boolean contains(T match);

    public T fetch(T match);
}

class UnresolvedTupleException extends RuntimeException {
}

class TupleSetImpl<T extends Tuple> implements TupleSet<T> {
    private HashSet<T> tuples = new HashSet<T>();

    public synchronized void put(T tuple) {
        if (tuple.resolved()) {
            tuples.add(tuple);
        } else {
            throw new UnresolvedTupleException();
        }
    }

    public synchronized boolean contains(T match) {
        return tuples.contains(match);
    }

    public synchronized T fetch(T match) {
        for (Iterator<T> iter = tuples.iterator(); iter.hasNext();) {
            T t = iter.next();
            if (t.equals(match)) {
                iter.remove();
                return t;
            }
        }
        return null;
    }
}

public class VersionHistoryCleanup {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private final static Logger log = LoggerFactory.getLogger(VersionHistoryCleanup.class);
    private Set<String> unreferenced = new HashSet<String>();
    private TupleSet<StringPairTuple> references = new TupleSetImpl<StringPairTuple>();
    private int historiesCount;
    private long progressTimer;

    public VersionHistoryCleanup() {
    }

    public void traverse(Session session) throws RepositoryException {
        progressTimer = System.currentTimeMillis();
        traverseStorage(session.getRootNode().getNode("jcr:system/jcr:versionStorage"));
    }

    public void process() {
        unreferenced = unreachableHistories(unreferenced, references);
        references = null; // allow for this item to be GC'ed
    }

    public void report(Session session) throws RepositoryException {
        int emptyCount = 0;
        int unreferencedCount = 0;
        for (String uuid : unreferenced) {
            Node versionHistory = session.getNodeByUUID(uuid);
            long nChildNodes = versionHistory.getNodes().getSize();
            if (nChildNodes == -1) {
                throw new RepositoryException("Cannot determin number of nodes");
            }
            if (nChildNodes == 2 && versionHistory.hasNode("jcr:versionLabels") && versionHistory.hasNode("jcr:rootVersion")) {
                if (log.isDebugEnabled())
                    log.debug("empty history: " + versionHistory.getPath());
                ++emptyCount;
            } else {
                if (log.isDebugEnabled())
                    log.debug("unreferenced history: " + versionHistory.getPath());
                ++unreferencedCount;
            }
        }
        log.info(unreferencedCount + " unreferenced histories and " + emptyCount + " empty histories totalling to " + (unreferencedCount + emptyCount) + " out of " + historiesCount);
    }

    public void repair(Session session) throws RepositoryException {
        for (String uuid : unreferenced) {
            try {
                session.refresh(false);
                Node node = session.getNodeByUUID(uuid);
                String path = node.getPath();
                node = org.hippoecm.repository.decorating.NodeDecorator.unwrap(node);
                node = org.hippoecm.repository.impl.NodeDecorator.unwrap(node);
                InternalVersionHistoryImpl internalVersionHistory = ((InternalVersionHistoryImpl)((VersionHistoryImpl)node).getInternalVersionHistory());
                ((InternalVersionManagerImpl)internalVersionHistory.getVersionManager()).removeVersionHistory((VersionHistory)node);
                session.save();
                log.info("removed version history " + uuid + " " + path);
            } catch (RepositoryException ex) {
                log.error("unable to remove version history", ex);
            }
        }
    }

    private static Set<String> unreachableHistories(Set<String> unreferenced, TupleSet<StringPairTuple> allReferences) {
        if (log.isDebugEnabled())
            log.debug("processing history links");
        TupleSet<StringPairTuple> references = new TupleSetImpl<StringPairTuple>();
        TupleSet<StringTuple> referenced = new TupleSetImpl<StringTuple>();
        StringPairTuple current;
        if (log.isDebugEnabled())
            log.debug("collecting primary links");
        while ((current = allReferences.fetch(new StringPairTuple(null, null))) != null) {
            if (unreferenced.contains(current.getSource())) {
                references.put(current);
            } else {
                referenced.put(new StringTuple(current.getTarget()));
            }
        }
        if (log.isDebugEnabled())
            log.debug("marking reachable histories ");
        StringTuple source;
        while ((source = referenced.fetch(new StringTuple(null))) != null) {
            unreferenced.remove(source.getItem());
            while ((current = references.fetch(new StringPairTuple(source.getItem(), null))) != null) {
                referenced.put(new StringTuple(current.getTarget()));
            }
        }
        return unreferenced;
    }

    private void versionedChilds(VersionHistory versionHistory, Node node) throws RepositoryException {
        if (node.isNodeType("nt:versionedChild")) {
            //String childVersionHistory = node.getProperty("jcr:childVersionHistory").getNode().getProperty("jcr:versionableUuid").getString();
            String childVersionHistory = node.getProperty("jcr:childVersionHistory").getString();
            references.put(new StringPairTuple(versionHistory.getUUID(), childVersionHistory));
        } else if (node.isNodeType("nt:frozenNode")) {
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                versionedChilds(versionHistory, child);
            }
        } else {
            log.warn("node " + node.getPath() + " not a nt:frozenNode or nt:versionedChild node");
        }
    }

    private void investigate(Node node) throws RepositoryException {
        if (!node.isNodeType("nt:frozenNode")) {
            throw new RepositoryException("node " + node.getPath() + " not a nt:frozenNode node");
        }
        if (!node.hasProperty("jcr:frozenPrimaryType")) {
            throw new RepositoryException("node " + node.getPath() + " is a malformed nt:frozenNode node");
        }
        if (log.isDebugEnabled())
            log.trace("traversing frozen " + node.getPath());
        VersionHistory versionHistory = (VersionHistory)node.getParent().getParent();
        versionedChilds(versionHistory, node);
        /*
         * import javax.jcr.*; NodeType nodeType =
         * node.getSession().getWorkspace().getNodeTypeManager().getNodeType(node.getProperty("jcr:frozenPrimaryType").getString());
         * try { Node referenced = node.getSession().getNodeByUUID(node.getProperty("jcr:frozenUuid").getString()); return; } catch
         * (ItemNotFoundException ex) { }
         */
    }

    private void traverseVersion(Node node) throws RepositoryException {
        if (!node.isNodeType("nt:version")) {
            throw new RepositoryException("node " + node.getPath() + " not a nt:version node");
        }
        if (log.isDebugEnabled())
            log.trace("traversing version " + node.getPath());
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (child.isNodeType("nt:frozenNode")) {
                investigate(child);
            } else {
                log.warn("node " + child.getPath() + " is of unrecognized node type " + child.getPrimaryNodeType());
            }
        }
    }

    private void traverseHistory(Node node) throws RepositoryException {
        if (!node.isNodeType("nt:versionHistory")) {
            throw new RepositoryException("node " + node.getPath() + " not a nt:versionHistory node");
        }
        ++historiesCount;
        if (System.currentTimeMillis() - progressTimer >= 10000) {
            log.info("loaded " + historiesCount + " version histories");
            progressTimer = System.currentTimeMillis();
        }
        VersionHistory versionHistory = (VersionHistory)node;
        try {
            versionHistory.getSession().getNodeByUUID(versionHistory.getVersionableUUID()); // ignore return value, programming by exception
        } catch (ItemNotFoundException ex) {
            if (log.isDebugEnabled())
                log.trace("no current found for version history " + versionHistory.getPath());
            unreferenced.add(node.getUUID());
        }
        if (log.isDebugEnabled())
            log.trace("traversing history " + node.getPath());
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (child.isNodeType("nt:versionLabels")) {
                // fine, just skip
            } else if (child.isNodeType("nt:version")) {
                traverseVersion(child);
            } else {
                log.warn("node " + child.getPath() + " is of unrecognized node type " + child.getPrimaryNodeType());
            }
        }
    }

    private void traverseStorage(Node node) throws RepositoryException {
        if (!node.isNodeType("rep:versionStorage")) {
            throw new RepositoryException("node " + node.getPath() + " not a rep:versionStorage node");
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (child.isNodeType("rep:versionStorage")) {
                traverseStorage(child);
            } else if (child.isNodeType("nt:versionHistory")) {
                traverseHistory(child);
            } else {
                log.warn("child " + node.getPath() + " is of unrecognized node type " + child.getPrimaryNodeType());
            }
        }
    }
}

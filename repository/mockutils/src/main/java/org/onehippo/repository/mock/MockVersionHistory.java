/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.NodeIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.LabelExistsVersionException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.jackrabbit.value.NameValue;
import org.onehippo.repository.util.JcrConstants;

/**
 * Mock version of {@link VersionHistory}.
 */
public class MockVersionHistory extends MockNode implements VersionHistory {

    private final MockNode versionable;
    private int versionCount;

    public MockVersionHistory(final MockNode versionable) throws RepositoryException {
        super(versionable.getIdentifier(), JcrConstants.NT_VERSION_HISTORY);
        this.versionable = versionable;
        versionCount = 0;

        final MockNode frozenRoot = new MockNode(JcrConstants.JCR_FROZEN_NODE, JcrConstants.NT_FROZEN_NODE);
        setFrozenProperties(frozenRoot);
        addVersion(JcrConstants.JCR_ROOT_VERSION, frozenRoot);
    }

    MockVersion addVersion() throws RepositoryException {
        final MockNode frozenNode = new MockNode(JcrConstants.JCR_FROZEN_NODE, JcrConstants.NT_FROZEN_NODE, versionable);
        frozenNode.getProperty(JcrConstants.JCR_IS_CHECKED_OUT).remove();
        frozenNode.setProperty(JcrConstants.JCR_FROZEN_UUID, versionable.getIdentifier());
        frozenNode.setProperty(JcrConstants.JCR_FROZEN_PRIMARY_TYPE, NameValue.valueOf(versionable.getPrimaryNodeType().getName()));
        freezeMixins(frozenNode);

        final String versionName = "1." + versionCount;
        MockVersion version = addVersion(versionName, frozenNode);
        versionCount += 1;
        return version;
    }

    private void freezeMixins(final MockNode frozenNode) throws RepositoryException {
        if (versionable.getMixinNodeTypes().length > 1) {
            Value[] mixinTypes = new Value[versionable.getMixinNodeTypes().length - 1];
            int pos = 0;
            for (NodeType nodeType : versionable.getMixinNodeTypes()) {
                if (nodeType.getName().equals(JcrConstants.MIX_VERSIONABLE)) {
                    continue;
                }
                mixinTypes[pos] = NameValue.valueOf(nodeType.getName());
                pos++;
            }
            frozenNode.setProperty(JcrConstants.JCR_FROZEN_MIXIN_TYPES, mixinTypes);
        }
    }

    private MockVersion addVersion(final String name, final MockNode frozenNode) throws RepositoryException {
        final MockVersion version = new MockVersion(name, JcrConstants.NT_VERSION, this);
        version.addNode(frozenNode);
        addNode(version);
        return version;
    }

    private MockNode setFrozenProperties(final MockNode frozen) throws RepositoryException {
        frozen.setProperty(JcrConstants.JCR_FROZEN_PRIMARY_TYPE, versionable.getPrimaryNodeType().getName());
        frozen.setProperty(JcrConstants.JCR_FROZEN_UUID, versionable.getIdentifier());
        return frozen;
    }

    @Override
    public String getVersionableUUID() throws RepositoryException {
        return getVersionableIdentifier();
    }

    @Override
    public String getVersionableIdentifier() throws RepositoryException {
        return versionable.getIdentifier();
    }

    @Override
    public MockVersion getRootVersion() throws RepositoryException {
        return (MockVersion) getNode(JcrConstants.JCR_ROOT_VERSION);
    }

    @Override
    public VersionIterator getAllLinearVersions() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionIterator getAllVersions() throws RepositoryException {
        List<MockVersion> versions = new ArrayList();

        NodeIterator nodes = getNodes("1.*");
        while (nodes.hasNext()) {
            versions.add((MockVersion)nodes.nextNode());
        }

        Collections.sort(versions, new NewToOldVersionComparator());

        return new MockVersionIterator(versions);
    }

    @Override
    public NodeIterator getAllLinearFrozenNodes() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator getAllFrozenNodes() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getVersion(final String versionName) throws VersionException, RepositoryException {
        if (!hasNode(versionName)) {
            throw new VersionException("No such version: " + versionName);
        }
        return (MockVersion) getNode(versionName);
    }

    @Override
    public Version getVersionByLabel(final String label) throws VersionException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addVersionLabel(final String versionName, final String label, final boolean moveLabel) throws LabelExistsVersionException, VersionException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVersionLabel(final String label) throws VersionException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasVersionLabel(final String label) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasVersionLabel(final Version version, final String label) throws VersionException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getVersionLabels() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getVersionLabels(final Version version) throws VersionException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVersion(final String versionName) throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        throw new UnsupportedOperationException();
    }


    private static class NewToOldVersionComparator implements Comparator<MockVersion> {
        @Override
        public int compare(final MockVersion v1, final MockVersion v2) {
            return v2.getName().compareTo(v1.getName());
        }
    }


    private static class MockVersionIterator implements VersionIterator {

        private long size;
        private Iterator<MockVersion> iterator;

        private MockVersionIterator(List<MockVersion> versions) {
            size = versions.size();
            iterator = versions.iterator();
        }

        @Override
        public Version nextVersion() {
            return iterator.next();
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Object next() {
            return iterator.next();
        }

        @Override
        public long getPosition() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void skip(final long skipNum) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}

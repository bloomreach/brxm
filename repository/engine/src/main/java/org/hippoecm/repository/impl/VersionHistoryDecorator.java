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
package org.hippoecm.repository.impl;

import javax.jcr.AccessDeniedException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

public class VersionHistoryDecorator extends NodeDecorator implements VersionHistory {

    protected final VersionHistory versionHistory;

    public static VersionHistory unwrap(final VersionHistory versionHistory) {
        if (versionHistory instanceof VersionHistoryDecorator) {
            return ((VersionHistoryDecorator)versionHistory).versionHistory;
        }
        return versionHistory;
    }

    VersionHistoryDecorator(final SessionDecorator session, final VersionHistory versionHistory) {
        super(session, versionHistory);
        this.versionHistory = unwrap(versionHistory);
    }

    public String getVersionableUUID() throws RepositoryException {
        return versionHistory.getVersionableUUID();
    }

    public VersionDecorator getRootVersion() throws RepositoryException {
        final Version version = versionHistory.getRootVersion();
        return new VersionDecorator(session, version);
    }

    public VersionIteratorDecorator getAllVersions() throws RepositoryException {
        return new VersionIteratorDecorator(session, versionHistory.getAllVersions());
    }

    public VersionDecorator getVersion(final String versionName) throws VersionException, RepositoryException {
        final Version version = versionHistory.getVersion(versionName);
        return new VersionDecorator(session, version);
    }

    public VersionDecorator getVersionByLabel(final String label) throws RepositoryException {
        final Version version = versionHistory.getVersionByLabel(label);
        return new VersionDecorator(session, version);
    }

    public void addVersionLabel(final String versionName, final String label, final boolean moveLabel) throws VersionException,
            RepositoryException {
        versionHistory.addVersionLabel(versionName, label, moveLabel);
    }

    public void removeVersionLabel(final String label) throws VersionException, RepositoryException {
        versionHistory.removeVersionLabel(label);
    }

    public boolean hasVersionLabel(final String label) throws RepositoryException {
        return versionHistory.hasVersionLabel(label);
    }

    public boolean hasVersionLabel(final Version version, final String label) throws VersionException, RepositoryException {
        return versionHistory.hasVersionLabel(VersionDecorator.unwrap(version), label);
    }

    public String[] getVersionLabels() throws RepositoryException {
        return versionHistory.getVersionLabels();
    }

    public String[] getVersionLabels(final Version version) throws VersionException, RepositoryException {
        return versionHistory.getVersionLabels(VersionDecorator.unwrap(version));
    }

    public void removeVersion(final String versionName) throws ReferentialIntegrityException, AccessDeniedException,
            UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        versionHistory.removeVersion(versionName);
    }

    public String getVersionableIdentifier() throws RepositoryException {
        return versionHistory.getVersionableIdentifier();
    }

    public VersionIteratorDecorator getAllLinearVersions() throws RepositoryException {
        return new VersionIteratorDecorator(session, versionHistory.getAllLinearVersions());
    }

    public NodeIteratorDecorator getAllLinearFrozenNodes() throws RepositoryException {
        return new NodeIteratorDecorator(session, versionHistory.getAllLinearFrozenNodes());
    }

    public NodeIteratorDecorator getAllFrozenNodes() throws RepositoryException {
        return new NodeIteratorDecorator(session, versionHistory.getAllFrozenNodes());
    }

    @Override
    public boolean recomputeDerivedData() throws RepositoryException {
        return false;
    }
}

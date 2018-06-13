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

import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

public class VersionDecorator extends NodeDecorator implements Version {

    protected final Version version;

    public static Version unwrap(final Version version) {
        if (version instanceof VersionDecorator) {
            return ((VersionDecorator) version).version;
        }
        return version;
    }

    VersionDecorator(final SessionDecorator session, final Version version) {
        super(session, version);
        this.version = unwrap(version);
    }

    public VersionHistoryDecorator getContainingHistory() throws RepositoryException {
        final VersionHistory vHistory = version.getContainingHistory();
        return new VersionHistoryDecorator(session, vHistory);
    }

    public Calendar getCreated() throws RepositoryException {
        return version.getCreated();
    }

    public VersionDecorator[] getSuccessors() throws RepositoryException {
        final Version[] successors = version.getSuccessors();
        final VersionDecorator[] result = new VersionDecorator[successors.length];
        for (int i = 0; i < successors.length; i++) {
            result[i] = new VersionDecorator(session, successors[i]);
        }
        return result;
    }

    public VersionDecorator[] getPredecessors() throws RepositoryException {
        final Version[] predecessors = version.getPredecessors();
        final VersionDecorator[] result = new VersionDecorator[predecessors.length];
        for (int i = 0; i < predecessors.length; i++) {
            result[i] = new VersionDecorator(session, predecessors[i]);
        }
        return result;
    }

    public VersionDecorator getLinearSuccessor() throws RepositoryException {
        return new VersionDecorator(session, version.getLinearSuccessor());
    }

    public VersionDecorator getLinearPredecessor() throws RepositoryException {
        return new VersionDecorator(session, version.getLinearPredecessor());
    }

    public NodeDecorator getFrozenNode() throws RepositoryException {
        return new NodeDecorator(session, version.getFrozenNode());
    }

    public boolean recomputeDerivedData() throws RepositoryException {
        return false;
    }
}

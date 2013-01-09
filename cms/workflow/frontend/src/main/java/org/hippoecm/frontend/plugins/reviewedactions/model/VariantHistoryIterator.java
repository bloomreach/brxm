/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.model;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

/**
 * Iterator over variants versions under a handle history.  Can filter on properties,
 * such as those found in a discriminator.  The results are primarily ordered by variant,
 * secondary by version.
 */
public class VariantHistoryIterator implements Iterator<Version> {

    private final Map<String, String> criteria;

    private Version current;
    private Version next;
    private Version handleVersion;
    private VersionIterator handleIter;
    private NodeIterator variantIterator;
    private VersionIterator variantHistoryIterator;
    private RepositoryException lastException;
    private Set<String> variants;

    public VariantHistoryIterator(Node handle, Map<String, String> criteria) throws RepositoryException {
        this.criteria = criteria;
        this.handleIter = handle.getVersionHistory().getAllVersions();
        this.variants = new TreeSet<String>();
    }

    private void fetch() throws RepositoryException {
        if (next == null) {
            // retry loop
            // when the innermost iterator has no more results, it is set to
            // null and the loop is restarted.  The parent is then used to get the
            // next inner iterator.
            while (true) {
                if (handleIter == null) {
                    return;
                }
                if (variantIterator == null) {
                    while (handleIter.hasNext()) {
                        handleVersion = handleIter.nextVersion();
                        if (!handleVersion.getName().equals("jcr:rootVersion")) {
                            variantIterator = handleVersion.getNode("jcr:frozenNode").getNodes();
                            break;
                        }
                    }
                    if (variantIterator == null) {
                        handleIter = null;
                        continue;
                    }
                }
                if (variantHistoryIterator == null) {
                    while (variantIterator.hasNext()) {
                        Node child = variantIterator.nextNode();
                        if (child.isNodeType("nt:versionedChild")) {
                            String ref = child.getProperty("jcr:childVersionHistory").getString();
                            if (variants.contains(ref)) {
                                continue;
                            } else {
                                variants.add(ref);
                            }
                            VersionHistory variantHistory = (VersionHistory) child.getSession().getNodeByUUID(ref);
                            variantHistoryIterator = variantHistory.getAllVersions();
                            break;
                        }
                    }
                    if (variantHistoryIterator == null) {
                        variantIterator = null;
                        continue;
                    }
                }
                while (variantHistoryIterator.hasNext()) {
                    Version version = variantHistoryIterator.nextVersion();
                    if (!version.getName().equals("jcr:rootVersion")) {
                        boolean match = true;
                        for (Map.Entry<String, String> entry : criteria.entrySet()) {
                            Node variant = version.getNode("jcr:frozenNode");
                            if (!variant.hasProperty(entry.getKey())
                                    || !variant.getProperty(entry.getKey()).getString().equals(entry.getValue())) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            next = version;
                            return;
                        }
                    }
                }
                variantHistoryIterator = null;
                continue;
            }
        }
    }

    public boolean hasNext() {
        try {
            fetch();
        } catch (RepositoryException ex) {
            lastException = ex;
        }
        return next != null;
    }

    public Version getHandleVersion() {
        return handleVersion;
    }

    public Version next() {
        if (hasNext()) {
            current = next;
            next = null;
            return current;
        }
        if (lastException == null) {
            throw new NoSuchElementException();
        } else {
            throw new NoSuchElementException(lastException.getMessage());
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

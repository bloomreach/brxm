/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * Helper methods for implementing editors and editor factories.
 */
public final class EditorHelper {
    private static final long serialVersionUID = 1L;

    private EditorHelper() {
    }

    public static Set<Node> getDocuments(Node handle) throws RepositoryException {
        Set<Node> variants = new HashSet<Node>();
        if (handle.isNodeType("nt:version")) {
            Calendar date = handle.getProperty("jcr:created").getDate();
            Node frozen = handle.getNode("jcr:frozenNode");
            NodeIterator variantHistoryReferences = frozen.getNodes();
            while (variantHistoryReferences.hasNext()) {
                Node variant = variantHistoryReferences.nextNode();
                if (!variant.isNodeType("nt:versionedChild")) {
                    continue;
                }
                Calendar latestDate = null;
                Node latestVariant = null;
                Node history = variant.getProperty("jcr:childVersionHistory").getNode();
                NodeIterator variantHistoryIter = history.getNodes("*");
                while (variantHistoryIter.hasNext()) {
                    Node variantVersion = variantHistoryIter.nextNode();
                    if (!variantVersion.isNodeType("nt:version")) {
                        continue;
                    }
                    Calendar variantVersionDate = variantVersion.getProperty("jcr:created").getDate();
                    if (variantVersionDate.compareTo(date) <= 0
                            && (latestDate == null || variantVersionDate.compareTo(latestDate) > 0)) {
                        latestDate = variantVersionDate;
                        latestVariant = variantVersion;
                    }
                }
                if (latestVariant != null) {
                    variants.add(latestVariant);
                }
            }
        } else {
            for (NodeIterator iter = handle.getNodes(handle.getName()); iter.hasNext();) {
                Node variant = iter.nextNode();
                variants.add(variant);
            }
        }
        return variants;
    }

}

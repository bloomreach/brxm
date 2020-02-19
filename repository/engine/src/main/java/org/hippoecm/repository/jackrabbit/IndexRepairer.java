/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.query.lucene.CachingMultiIndexReader;
import org.apache.jackrabbit.core.query.lucene.ConsistencyCheck;
import org.apache.jackrabbit.core.query.lucene.ConsistencyCheckError;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.FieldSelectors;
import org.apache.jackrabbit.core.query.lucene.MultiIndex;
import org.apache.jackrabbit.core.query.lucene.MultiIndexAccessor;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.lucene.document.Document;
import org.hippoecm.repository.query.lucene.ServicingSearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptyList;

public class IndexRepairer {

    private static final Logger log = LoggerFactory.getLogger(IndexRepairer.class);

    private ServicingSearchIndex searchIndex;

    public IndexRepairer(final ServicingSearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    public List<ConsistencyCheckError> repairInconsistencies() {

        final long start = System.currentTimeMillis();

        log.info("Start index consistency check");

        try {
            final ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
            List<ConsistencyCheckError> errors = consistencyCheck.getErrors();
            if (!errors.isEmpty()) {
                consistencyCheck.doubleCheckErrors();
                errors = consistencyCheck.getErrors();
                if (!errors.isEmpty()) {
                    log.warn("Found '{}' index errors", errors.size());

                    consistencyCheck.repair(true);

                    for (ConsistencyCheckError error : errors) {
                        if (error.repairable()) {
                            log.warn("Found index error '{}'. Error has been fixed", error.toString());
                        } else {
                            log.error("Found index error '{}'. Error cannot be fixed", error.toString());
                        }
                    }
                    return errors;
                } else {
                    log.info("No errors detected");
                }
            } else {
                log.info("No errors detected");
            }

        } catch (IOException e) {
            log.error("Search index repair failed", e);
        } finally {
            log.info("Finished index repair in {} ms", System.currentTimeMillis() - start);
        }
        return emptyList();
    }

    public List<String> repairDuplicates() {

        final long start = System.currentTimeMillis();
        final List<String> messages = new ArrayList<>();

        try {
            CachingMultiIndexReader reader = null;
            try {

                final MultiIndex multiIndex = searchIndex.getIndex();
                reader = multiIndex.getIndexReader();

                final Set<UUID> duplicateEntries = new HashSet<>();
                // avoid rehashing since this set can contain many millions of items
                // note use numDocs instead of maxDoc since numdocs contains actual number
                final Set<UUID> allEntries = new HashSet<>((int) Math.ceil(reader.numDocs() / 0.75));

                int logProgressEvery = reader.maxDoc() / 10;
                int logProgressAt = logProgressEvery;
                int progress = 0;

                for (int i = 0; i < reader.maxDoc(); i++) {
                    if (i == logProgressAt) {
                        logProgressAt += logProgressEvery;
                        progress += 1;
                        log.info("progress: " + progress * 10 + "%");
                    }
                    if (reader.isDeleted(i)) {
                        continue;
                    }
                    Document d = reader.document(i, FieldSelectors.UUID);
                    // use uuid for minimal memory consumption
                    final UUID uuid = UUID.fromString(d.get(FieldNames.UUID));
                    if (!allEntries.add(uuid)) {
                        log.warn("Found duplicate index entry for '{}'", uuid);
                        duplicateEntries.add(uuid);
                    }
                }

                for (UUID uuid : duplicateEntries) {
                    // first remove all occurrences
                    final NodeId id = new NodeId(uuid);

                    MultiIndexAccessor.removeAllDocuments(multiIndex, id);
                    // then re-index the node
                    try {
                        NodeState node = (NodeState) searchIndex.getContext().getItemStateManager().getItemState(id);
                        Document d = MultiIndexAccessor.createDocument(multiIndex, node);
                        MultiIndexAccessor.addDocument(multiIndex, d);
                    } catch (NoSuchItemStateException e) {
                        log.info("Not re-indexing node with multiple occurrences because node no longer exists");
                    } catch (RepositoryException | ItemStateException | IOException e) {
                        if (log.isInfoEnabled()) {
                            log.info("Could not reindex node with multiple occurrences", e);
                        } else {
                            log.info("Could not reindex node with multiple occurrences : {}", e.toString());
                        }
                    }
                    messages.add(String.format("Fixed duplicate entry for node id '%s'", uuid));
                }

            } finally {
                if (reader != null) {
                    reader.release();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not create IndexReader", e);
        } finally {
            log.info("Finished index duplicate repair in {} ms", System.currentTimeMillis() - start);
        }
        return messages;
    }


}

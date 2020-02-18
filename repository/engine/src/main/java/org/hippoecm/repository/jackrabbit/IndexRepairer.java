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
import java.util.Collections;
import java.util.List;

import org.apache.jackrabbit.core.query.lucene.ConsistencyCheck;
import org.apache.jackrabbit.core.query.lucene.ConsistencyCheckError;
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
        
        if (Boolean.getBoolean("disableStartupIndexConsistencyCheck")) {
            log.info("Explicit system property 'disableStartupIndexConsistencyCheck' set to true, skipping check");
            return emptyList();
        } else {
            log.info("Start index consistency check");
        }
        try {
            final ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
            List<ConsistencyCheckError> errors = consistencyCheck.getErrors();
            if (!errors.isEmpty()) {
                consistencyCheck.doubleCheckErrors();
                errors = consistencyCheck.getErrors();
                if (!errors.isEmpty()) {
                    log.info("Found '{}' index errors", errors.size());

                    consistencyCheck.repair(true);

                    for (ConsistencyCheckError error : errors) {
                        if (error.repairable()) {
                            log.info("Found index error '{}'. Error has been fixed", error.toString());
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
            log.error("Search index consistency check failed", e);
        } finally {
            log.info("Finished index consistency checker in {} ms", System.currentTimeMillis() - start);
        }
        return emptyList();
    }
}

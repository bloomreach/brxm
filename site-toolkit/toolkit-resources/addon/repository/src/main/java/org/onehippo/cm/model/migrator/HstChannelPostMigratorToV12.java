/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.migrator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cm.engine.migrator.MigrationException;
import org.onehippo.cm.engine.migrator.PostMigrator;
import org.onehippo.cm.model.ConfigurationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PostMigrator
public class HstChannelPostMigratorToV12 extends HstChannelMigratorToV12 {

    private static final Logger log = LoggerFactory.getLogger(HstChannelPostMigratorToV12.class);

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    protected boolean preemptiveInitializeHstNodeType(final Session session, final ConfigurationModel configurationModel) throws RepositoryException {
        // post migrator never has to reload node types for hst. Just return success
        return true;
    }

    @Override
    protected boolean shouldRun(final Session session, final boolean autoExportEnabled) throws RepositoryException {
        final boolean shouldRun = super.shouldRun(session, autoExportEnabled);
        if (!shouldRun) {
            return false;
        }

        // This means that the HstChannelPreMigratorToV12 did NOT relocate the hst:hst/hst:channels nodes, meaning they got
        // bootstrapped by the Configuration Model. This means we want to run the post migrator with auto export enabled
        // to rewrite local yaml configuration. If auto export is disabled, we short-circuit repository startup because
        // auto export must be enabled.
        if (!autoExportEnabled) {
            // we log a big ERROR that the HST configuration is in PRE-CMS12 configuration style and that you need to enable auto export
            throw new MigrationException("Auto Export must be enabled because the bootstrap yaml configuration still has the node /hst:hst/hst:channels. " +
                    "This is the PRE-CMS12 configuration which needs to be migrated.");
        }
        log.info("Starting {}", this);
        return true;
    }
}

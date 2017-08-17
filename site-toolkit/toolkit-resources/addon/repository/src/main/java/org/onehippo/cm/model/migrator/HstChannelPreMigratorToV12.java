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

import org.onehippo.cm.engine.migrator.PreMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PreMigrator
public class HstChannelPreMigratorToV12 extends HstChannelMigratorToV12 {

    private static final Logger log = LoggerFactory.getLogger(HstChannelPreMigratorToV12.class);

    @Override
    public Logger getLogger() {
        return log;
    }

    public HstChannelPreMigratorToV12() {
        super();
    }

    public HstChannelPreMigratorToV12(final String hstRoot, final boolean save) {
        super(hstRoot, save);
    }


}

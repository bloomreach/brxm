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
package org.onehippo.cm.engine.migrator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cm.model.ConfigurationModel;

/**
 * <p>
 *    {@link ConfigurationMigrator}s run <strong>after</strong> the {@link ConfigurationModel} is loaded but before the {@link ConfigurationModel}
 *    is applied to the JCR Nodes (thus before applied to config or content). Be aware that {@link ConfigurationMigrator}s always
 *    run at startup hence should always have a fast initial check whether they have any work to do!
 * </p>
 * <p>
 *    For a migrator to run it has to implement this interface and have the {@link PreMigrator} class annotation and be
 *    in one of the hippo internal packages. There is no specific order in which migrators run so a migrator should not
 *    rely on other migrators.
 * </p>
 * <p>
 *     A {@link ConfigurationMigrator} implementation must have no-arg public constructor
 * </p>
 */
public interface ConfigurationMigrator {

    /**
     * Run a migration of JCR data that must be executed before applying HCM config changes. This method is expected
     * to receive a clean Session (with no pending changes) and to save() its own changes, if necessary. Changes that
     * are not save()d will be discarded by the caller.
     * @return {@code true} if something changed as a result of this migrator (used only as a hint for the caller)
     */
    boolean migrate(final Session session, final ConfigurationModel configurationModel) throws RepositoryException;
}

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
 *    {@link JcrMigrator}s run <strong>after</strong> the {@link ConfigurationModel} is loaded but before the {@link ConfigurationModel}
 *    is applied to the JCR Nodes (thus before applied to config or content). Be aware that {@link JcrMigrator}s always
 *    run at startup hence should always have an initial check whether they should run!
 * </p>
 * <p>
 *    For a Migrator to run it has to implement this interface and have the {@link Migrator} class annotation and be
 *    in a certain package. There is no specific order in which migrators run so a migrator should not rely on other migrators.
 * </p>
 * <p>
 *     A {@link Migrator} implementation must have no-arg public constructor
 * </p>
 */
public interface JcrMigrator {

    /**
     * @return {@code true} if something changed as a result of this migrator
     */
    boolean migrate(final Session session, final ConfigurationModel configurationModel) throws RepositoryException;
}

/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public abstract class InitializeInstruction {

    protected final InitializeItem item;
    protected final Session session;

    protected InitializeInstruction(final InitializeItem item, final Session session) {
        this.item = item;
        this.session = session;
    }

    protected abstract String getName();

    protected boolean canCombine(InitializeInstruction instruction) {
        return false;
    }

    public abstract PostStartupTask execute() throws RepositoryException;

    @Override
    public String toString() {
        try {
            return item.getName() + "/" + getName();
        } catch (RepositoryException ignore) {
            return getName();
        }
    }
}

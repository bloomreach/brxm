/*
 *  Copyright 2013-2023 Bloomreach
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
package org.onehippo.repository.scheduling;

/**
 * Base class for information about when to schedule a job with the repository.
 * <p>
 * A trigger is associated with one job but a single job can have multiple triggers.
 * Every trigger associated with the same job must have a different name.
 * </p>
 */
public abstract class RepositoryJobTrigger {

    private final String name;

    public RepositoryJobTrigger(final String name) {
        this.name = name;
    }

    /**
     * @return the name of the trigger.
     */
    public String getName() {
        return name;
    }

}

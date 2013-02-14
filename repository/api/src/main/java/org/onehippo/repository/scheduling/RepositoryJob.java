/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;

/**
 * Interface to implement to perform the work to be scheduled.
 */
public interface RepositoryJob {

    /**
     * Job execution callback.
     *
     * @param context  operational context object.
     * @throws RepositoryException  when an error occurs.
     */
    public void execute(RepositoryJobExecutionContext context) throws RepositoryException;

}
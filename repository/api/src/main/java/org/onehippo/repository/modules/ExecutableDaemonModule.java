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
package org.onehippo.repository.modules;


import javax.jcr.RepositoryException;

/**
 * An ExecutableDaemonModule is a DaemonModule that is executed once.
 * After the execute method has run to completion, i.e. without cancel
 * having been called, the module is marked as executed in the repository
 * and not run again. The repository also makes sure that only one
 * cluster node executes the module.
 *
 * <p>
 * For long running jobs implementers must make sure cancel actually
 * causes execute to be cancelled and to return quickly. Otherwise
 * the repository will not be able to shut down until the module execution
 * has been completed.
 * </p>
 */
public interface ExecutableDaemonModule extends DaemonModule {

    void execute() throws RepositoryException;

    void cancel();

}

/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

public class Run {
    private final ModuleInfo[] modules;
    private final Validator preConditionValidator;
    private final JcrRunner jcrRunner;
    private final Validator postConditionValidator;

    public Run(final ModuleInfo module,
        final Validator preConditionValidator,
        final JcrRunner jcrRunner,
        final Validator postConditionValidator) {
        this(new ModuleInfo[]{module}, preConditionValidator, jcrRunner, postConditionValidator);
    }

    public Run(final ModuleInfo[] modules,
        final Validator preConditionValidator,
        final JcrRunner jcrRunner,
        final Validator postConditionValidator) {
        this.modules = modules;
        this.preConditionValidator = preConditionValidator;
        this.jcrRunner = jcrRunner;
        this.postConditionValidator = postConditionValidator;
    }

    public ModuleInfo[] getModules() {
        return modules;
    }

    public Validator getPreConditionValidator() {
        return preConditionValidator;
    }

    public JcrRunner getJcrRunner() {
        return jcrRunner;
    }

    public Validator getPostConditionValidator() {
        return postConditionValidator;
    }
}

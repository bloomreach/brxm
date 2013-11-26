/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.scxml;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.env.SimpleDispatcher;
import org.apache.commons.scxml2.env.Tracer;
import org.apache.commons.scxml2.env.jexl.JexlContext;
import org.apache.commons.scxml2.env.jexl.JexlEvaluator;
import org.apache.commons.scxml2.model.SCXML;

/**
 * RepositorySCXMLExecutorFactory
 */
public class RepositorySCXMLExecutorFactory implements SCXMLExecutorFactory {

    void initialize() {
    }

    @Override
    public SCXMLExecutor createSCXMLExecutor(SCXML scxml) throws SCXMLException {
        // TODO: refine evaluator, dispatcher, tracer, etc.
        SCXMLExecutor executor = new SCXMLExecutor(new JexlEvaluator(), new SimpleDispatcher(), new Tracer());
        executor.setRootContext(new JexlContext());
        executor.setStateMachine(scxml);
        return executor;
    }

    void destroy() {
    }

}

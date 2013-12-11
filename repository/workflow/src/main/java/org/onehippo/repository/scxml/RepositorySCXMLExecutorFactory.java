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

import org.apache.commons.scxml2.Evaluator;
import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.env.SimpleDispatcher;
import org.apache.commons.scxml2.env.jexl.JexlContext;
import org.apache.commons.scxml2.env.jexl.JexlEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RepositorySCXMLExecutorFactory
 */
public class RepositorySCXMLExecutorFactory implements SCXMLExecutorFactory {

    static Logger log = LoggerFactory.getLogger(RepositorySCXMLExecutorFactory.class);

    private Evaluator evaluator;

    void initialize() {
    }

    @Override
    public SCXMLExecutor createSCXMLExecutor(SCXMLDefinition scxmlDef) throws SCXMLException {
        if (this.evaluator == null) {
            JexlEvaluator evaluator = new JexlEvaluator();
            evaluator.setJexlEngineSilent(false);
            evaluator.setJexlEngineStrict(true);
            this.evaluator = evaluator;
        }

        final JexlContext jexlCtx = new JexlContext();
        SCXMLExecutor executor = new SCXMLExecutor(evaluator, new SimpleDispatcher(), new HippoScxmlErrorReporter(scxmlDef));
        executor.setRootContext(jexlCtx);
        executor.setStateMachine(scxmlDef.getSCXML());
        return executor;
    }

    void destroy() {
    }
}

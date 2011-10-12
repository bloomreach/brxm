/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.behavioral;

import java.util.Map;

/**
 * Main interface for the behavioral data evaluation engine. A {@link Persona} is associated with a set of rules
 * and an expression expresses that set of rules. Since the evaluation engine is a boolean engine the expressions
 * can only be boolean operators such as AND, OR and NOT. A special expression concerns an expression that evaluates
 * a {@link Rule} which can be likened to a predicate expression in predicate logic.
 */
public interface Expression {

    /**
     * @param data  the {@link BehavioralData} to evaluate.
     * @return  whether the expression evaluates to <code>true</code> or <code>false</code> 
     * given the {@link BehavioralData}
     */
    public boolean evaluate(Map<String, BehavioralData> data);

}

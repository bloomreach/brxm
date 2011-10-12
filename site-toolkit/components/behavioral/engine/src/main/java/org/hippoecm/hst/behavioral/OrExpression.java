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

import java.util.List;
import java.util.Map;

public class OrExpression implements Expression {
    
    private List<Expression> expressions;
    
    OrExpression(List<Expression> expressions) {
        if (expressions.size() < 2) {
            throw new IllegalArgumentException("OR expression must have 2 or more sub expressions");
        } 
        this.expressions = expressions;
    }
    
    @Override
    public boolean evaluate(Map<String, BehavioralData> data) {
        boolean result = false;
        for (Expression expression : expressions) {
            result |= expression.evaluate(data);
            if (result) {
                break;
            }
        }
        return result;
    }

}

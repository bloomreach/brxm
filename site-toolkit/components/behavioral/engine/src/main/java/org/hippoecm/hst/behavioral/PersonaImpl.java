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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class PersonaImpl implements Persona {

    private final String id;
    private final String name;
    
    private Expression expression;
    
    private final Configuration configuration;
    
    private List<Rule> rules = new ArrayList<Rule>();
    
    public PersonaImpl(Node jcrNode, Configuration configuration) throws RepositoryException {
        if (!jcrNode.isNodeType(BehavioralNodeTypes.BEHAVIORAL_NODETYPE_PERSONA)) {
            throw new IllegalArgumentException("Persona node not of the expected type. Expected '" + BehavioralNodeTypes.BEHAVIORAL_NODETYPE_PERSONA + "' but was '" + jcrNode.getPrimaryNodeType().getName() + "'");
        }
        this.configuration = configuration;
        
        this.id = jcrNode.getName();
        this.name = jcrNode.getProperty(BehavioralNodeTypes.BEHAVIORAL_GENERAL_PROPERTY_NAME).getString();
        
        NodeIterator expressionNodes = jcrNode.getNodes();
        if (expressionNodes.hasNext()) {
            Node expressionNode = expressionNodes.nextNode();
            if (expressionNodes.hasNext()) {
                throw new IllegalArgumentException("Persona can only have one expression subnode");
            }
            this.expression = createExpression(expressionNode);
        }
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Expression getExpression() {
        return expression;
    }
    
    List<Rule> getRules() {
        return rules;
    }

    private Expression createExpression(Node node) throws RepositoryException {
        if (node.getName().equals("or")) {
            List<Expression> subExpressions = new ArrayList<Expression>();
            NodeIterator subExpressionNodes = node.getNodes();
            while (subExpressionNodes.hasNext()) {
                subExpressions.add(createExpression(subExpressionNodes.nextNode()));
            }
            return new OrExpression(subExpressions);
        }
        if (node.getName().equals("and")) {
            List<Expression> subExpressions = new ArrayList<Expression>();
            NodeIterator subExpressionNodes = node.getNodes();
            while (subExpressionNodes.hasNext()) {
                subExpressions.add(createExpression(subExpressionNodes.nextNode()));
            }
            return new AndExpression(subExpressions);
        }
        if (node.getName().equals("not")) {
            NodeIterator subExpressionNodes = node.getNodes();
            if (subExpressionNodes.hasNext()) {
                Node subExpressionNode = subExpressionNodes.nextNode();
                if (subExpressionNodes.hasNext()) {
                    // TODO : incorrect configuration
                }
                return new NotExpression(createExpression(subExpressionNode));
            }
        }
        if (node.getName().equals("rule")) {
            RuleImpl rule = new RuleImpl(node);
            rules.add(rule);
            return new RuleExpression(rule, configuration.getDataProvider(rule.getProviderId()));
        }
        throw new IllegalArgumentException("Unrecognized expression configuration " + node.getName());
    }
}

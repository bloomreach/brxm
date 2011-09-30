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
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.behavioral.BehavioralNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Segment {

    private static final Logger log = LoggerFactory.getLogger(Segment.class);

    private final String id;
    private final String name;
    private final List<Rule> rules;
    private final Dimension dimension;
    
    public Segment(Node jcrNode, Dimension dimension) throws RepositoryException {
        if(!jcrNode.isNodeType(BehavioralNodeTypes.BEHAVIORAL_NODETYPE_SEGMENT)) {
            throw new IllegalArgumentException("Segment node not of the expected type. Expected '"+BehavioralNodeTypes.BEHAVIORAL_NODETYPE_SEGMENT+"' but was + '"+jcrNode.getPrimaryNodeType().getName()+"' ");
        }
        this.id = dimension.getId() + ":" + jcrNode.getName();
        this.name = jcrNode.getProperty(BehavioralNodeTypes.BEHAVIORAL_GENERAL_PROPERTY_NAME).getString();
        this.dimension = dimension;
        
        List<Rule> rules = new ArrayList<Rule>();

        NodeIterator ruleIter = jcrNode.getNodes();
        while (ruleIter.hasNext()) {
            Node ruleNode = ruleIter.nextNode();
            try {
                Rule rule = new Rule(ruleNode, this);
                rules.add(rule);
            } catch (RepositoryException e) {
                log.error("Unable to create rule for segment " + name, e);
            } catch (IllegalArgumentException e) {
                log.error("Unable to create rule for segment " + name, e);
            }
        }
        this.rules = Collections.unmodifiableList(rules);
    }
    
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public Dimension getDimension() {
        return dimension;
    }

}

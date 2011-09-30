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
package org.onehippo.hst.behavioral;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.onehippo.hst.behavioral.BehavioralNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dimension {

    private static final Logger log = LoggerFactory.getLogger(Dimension.class);
    
    private final String id;
    private final String name;
    private final Map<String, Segment> segments;
    
    public Dimension(Node jcrNode) throws RepositoryException {
        if(!jcrNode.isNodeType(BehavioralNodeTypes.BEHAVIORAL_NODETYPE_DIMENSION)) {
            throw new IllegalArgumentException("Dimension node not of the expected type. Expected '"+BehavioralNodeTypes.BEHAVIORAL_NODETYPE_DIMENSION+"' but was + '"+jcrNode.getPrimaryNodeType().getName()+"' ");
        }
        this.id = jcrNode.getName();
        this.name = jcrNode.getProperty(BehavioralNodeTypes.BEHAVIORAL_GENERAL_PROPERTY_NAME).getString();
        
        Map<String, Segment> segments = new HashMap<String, Segment>();
        NodeIterator segmentIter = jcrNode.getNodes();
        while (segmentIter.hasNext()) {
            Node segmentNode = segmentIter.nextNode();
            try {
                Segment segment = new Segment(segmentNode, this);
                segments.put(segment.getId(), segment);
            } catch (RepositoryException e) {
                log.error("Unable to create segment for dimension " + name, e);
            } catch (IllegalArgumentException e) {
                log.error("Unable to create segment for dimension " + name, e);
            }
        }
        
        this.segments = Collections.unmodifiableMap(segments);
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public Map<String, Segment> getSegments() {
        return segments;
    }
    
}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Persona {

    private static final Logger log = LoggerFactory.getLogger(Persona.class);

    private final String id;
    private final String name;
    private final List<Segment> segments;
    
    public Persona(Node jcrNode, Configuration configuration) throws RepositoryException {
        if (!jcrNode.isNodeType(BehavioralNodeTypes.BEHAVIORAL_NODETYPE_PERSONA)) {
            throw new IllegalArgumentException("Persona node not of the expected type. Expected '" + BehavioralNodeTypes.BEHAVIORAL_NODETYPE_PERSONA + "' but was '" + jcrNode.getPrimaryNodeType().getName() + "'");
        }
        this.id = jcrNode.getName();
        this.name = jcrNode.getProperty(BehavioralNodeTypes.BEHAVIORAL_GENERAL_PROPERTY_NAME).getString();
        
        List<Segment> segments = new ArrayList<Segment>();
        for (Value segmentValue : jcrNode.getProperty(BehavioralNodeTypes.BEHAVIORAL_PERSONA_PROPERTY_SEGMENTS).getValues()) {
            String segmentString = segmentValue.getString();
            int indexOfColon = segmentString.indexOf(':');
            String dimensionName = segmentString.substring(0, indexOfColon);
            String segmentName = segmentString.substring(indexOfColon+1);
            Dimension dimension = configuration.getDimensions().get(dimensionName);
            if (dimension == null) {
                log.warn("No such segment: " + segmentString + ". Configuration of Persona " + getName() + " is broken.");
                continue;
            }
            Segment segment = dimension.getSegments().get(segmentName);
            if (segment == null) {
                log.warn("No such segment: " + segmentString + ". Configuration of Persona " + getName() + " is broken.");
                continue;
            }
            segments.add(segment);
        }
        this.segments = Collections.unmodifiableList(segments);
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public List<Segment> getSegments() {
        return segments;
    }

}

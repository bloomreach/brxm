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
package org.hippoecm.hst.behavioral.rest;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a list of personas.
 */
@XmlRootElement(name = "data")
public class PersonasRepresentation {

    private final List<PersonaRepresentation> personas;

    public PersonasRepresentation(Node node) throws RepositoryException {
        personas = new ArrayList<PersonaRepresentation>();
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            String id = child.getName();
            String name = child.getProperty("behavioral:name").getString();
            personas.add(new PersonaRepresentation(id, name, null, null));
        }
    }

    public List<PersonaRepresentation> getPersonas() {
        return personas;
    }

}

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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a list of personas.
 */
@XmlRootElement(name = "data")
public class PersonasRepresentation {

    private static final PersonaRepresentation[] HARDCODED_PERSONAS = {
            new PersonaRepresentation().represent("buyer", "Buyer", "A visitor that may be interested in buying one of our products."),
            new PersonaRepresentation().represent("customer", "Customer", "A logged-in user of our site."),
            new PersonaRepresentation().represent("default", "Default", "A visitor of our site that does not fit any of the other personas. This persona cannot be removed."),
            new PersonaRepresentation().represent("developer", "Developer", "A visitor that has come to our site by searching for technical terms in a search engine, or through a developer-oriented website."),
    };

    private List<PersonaRepresentation> personas;

    public PersonasRepresentation() {
        personas = new ArrayList<PersonaRepresentation>();
        personas.addAll(Arrays.asList(HARDCODED_PERSONAS));
    }

    public List<PersonaRepresentation> getPersonas() {
        return personas;
    }

    public void setPersonas(final List<PersonaRepresentation> personas) {
        this.personas = personas;
    }

}

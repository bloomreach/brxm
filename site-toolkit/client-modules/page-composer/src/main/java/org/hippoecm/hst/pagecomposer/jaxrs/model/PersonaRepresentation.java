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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a persona.
 */
@XmlRootElement(name = "persona")
public class PersonaRepresentation {

    private String id;
    private String name;
    private String description;
    private String avatarName;

    public PersonaRepresentation(String id, String name, String description, String avatarName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.avatarName = avatarName;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getAvatarName() {
        return avatarName;
    }

    public void setAvatarName(final String avatarName) {
        this.avatarName = avatarName;
    }

}
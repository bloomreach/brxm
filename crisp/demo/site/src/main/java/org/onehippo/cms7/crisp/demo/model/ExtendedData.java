/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.demo.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="extendedData")
public class ExtendedData {

    private String title;

    private String type;

    private String uri;

    private String description;

    @XmlElement
    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }

    @XmlElement
    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    @XmlElement
    public String getUri() {
        return uri;
    }

    public void setUri(String value) {
        this.uri = value;
    }

    @XmlElement
    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

}

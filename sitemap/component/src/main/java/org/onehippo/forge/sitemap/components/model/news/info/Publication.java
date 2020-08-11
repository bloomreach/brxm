/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.sitemap.components.model.news.info;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Wrapper class for the "publication" element in the google news sitemap
 * @author Wouter Danes
*/
@XmlRootElement(name = "publication")
@SuppressWarnings({"unused"})
public class Publication implements Cloneable {
    private String name;
    private String language;

    public Publication() {
    }

    public Publication(String name, String language) {
        this.name = name;
        this.language = language;
    }

    @XmlElement(required = true)
    public String getName() {
        return name;
    }

    @XmlElement(required = true)
    public String getLanguage() {
        return language;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Publication pub = (Publication) super.clone();
        pub.setLanguage(language);
        pub.setName(name);
        return pub;
    }
}
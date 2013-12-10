/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "items")
public class StepRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private String name;
    private String buttonText;

    private String description;
    private String template;

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(final String buttonText) {
        this.buttonText = buttonText;
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

    public String getTemplate() {
        return template;
    }

    public void setTemplate(final String template) {
        this.template = template;
    }
}

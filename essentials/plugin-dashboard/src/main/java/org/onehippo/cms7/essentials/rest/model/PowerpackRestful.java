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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.rest.Restful;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "powerpack")
public class PowerpackRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private List<StepRestful> steps = new ArrayList<>();
    private String name;
    private boolean enabled;
    private String introduction;
    private String template;
    private String value;


    public void addStep(final StepRestful step) {
        steps.add(step);
    }

    public List<StepRestful> getSteps() {
        return steps;
    }


    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public void setSteps(final List<StepRestful> steps) {
        this.steps = steps;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(final String introduction) {
        this.introduction = introduction;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(final String template) {
        this.template = template;
    }
}

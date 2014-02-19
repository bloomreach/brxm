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

import org.onehippo.cms7.essentials.dashboard.model.Restful;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "item")
public class ControllerRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private String id;
    private String controllerName;
    private String viewPath;

    public ControllerRestful(final String id, final String controllerName, final String viewPath) {
        this.id = id;
        this.controllerName = controllerName;
        this.viewPath = viewPath;
    }

    public ControllerRestful() {

    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getControllerName() {

        return controllerName;
    }

    public void setControllerName(final String controllerName) {
        this.controllerName = controllerName;
    }

    public String getViewPath() {
        return viewPath;
    }

    public void setViewPath(final String viewPath) {
        this.viewPath = viewPath;
    }
}

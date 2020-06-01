/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.richtext.link;

import java.io.Serializable;

import javax.jcr.Node;

import org.onehippo.cms7.services.htmlprocessor.model.Model;

public class RichTextLink implements Serializable {

    private final Model<Node> model;
    private String uuid;

    public RichTextLink(final Model<Node> model, final String uuid) {
        this.model = model;
        this.uuid = uuid;
    }

    public Model<Node> getTargetModel() {
        return model;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

}

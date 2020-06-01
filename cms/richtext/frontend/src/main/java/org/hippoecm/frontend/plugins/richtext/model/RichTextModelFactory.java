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
package org.hippoecm.frontend.plugins.richtext.model;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.onehippo.cms7.services.htmlprocessor.model.Model;

public class RichTextModelFactory {

    private final String processorId;

    public RichTextModelFactory(final String processorId) {
        this.processorId = processorId;
    }

    public IModel<String> create(final Model<String> valueModel, final Model<Node> nodeModel) {
        return new RichTextModel(processorId, valueModel, nodeModel);
    }
}

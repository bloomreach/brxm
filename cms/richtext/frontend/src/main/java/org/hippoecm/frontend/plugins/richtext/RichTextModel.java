/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketModel;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketNodeFactory;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketURLEncoder;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.model.RichTextProcessorModel;

public class RichTextModel implements IModel<String> {

    private final RichTextProcessorModel processorModel;
    private IModel<String> valueModel;
    private IModel<Node> nodeModel;

    public RichTextModel(final RichTextProcessorModel processorModel) {
        this.processorModel = processorModel;
    }

    public RichTextModel(final String processorId, final IModel<String> valueModel, final IModel<Node> nodeModel) {
        this.valueModel = valueModel;
        this.nodeModel = nodeModel;
        this.processorModel = new RichTextProcessorModel(WicketModel.of(valueModel), WicketModel.of(nodeModel),
                                                         HtmlProcessorFactory.of(processorId),
                                                         WicketNodeFactory.INSTANCE,
                                                         WicketURLEncoder.INSTANCE);
    }

    @Override
    public String  getObject() {
        return processorModel.get();
    }

    @Override
    public void setObject(final String value) {
        processorModel.set(value);
    }

    @Override
    public void detach() {
        if (valueModel != null) {
            valueModel.detach();
        }
        if (nodeModel != null) {
            nodeModel.detach();
        }
    }

}

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
package org.hippoecm.frontend.plugins.richtext;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.richtext.processor.WicketNodeFactory;
import org.hippoecm.frontend.plugins.richtext.processor.WicketURLEncoder;
import org.onehippo.cms7.services.processor.html.HtmlProcessorFactory;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.richtext.model.RichTextProcessorModel;

public class RichTextModel implements IModel<String> {

    private final RichTextProcessorModel processorModel;

    public RichTextModel(final RichTextProcessorModel processorModel) {
        this.processorModel = processorModel;
    }

    public RichTextModel(final String processorId, final Model<String> valueModel, final Model<Node> nodeModel) {
        this.processorModel = new RichTextProcessorModel(valueModel, nodeModel,
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
        processorModel.release();
    }

}

/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.model.IModel;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorFactory;
import org.onehippo.cms7.services.htmlprocessor.model.HtmlProcessorModel;
import org.onehippo.cms7.services.htmlprocessor.model.Model;

public class HtmlModel implements IModel<String> {

    private final HtmlProcessorModel processorModel;

    public HtmlModel(final String processorId, final Model<String> valueModel) {
        this.processorModel = new HtmlProcessorModel(valueModel, HtmlProcessorFactory.of(processorId));
    }

    @Override
    public String getObject() {
        return processorModel.get();
    }

    @Override
    public void setObject(final String value) {
        processorModel.set(value);
    }

    @Override
    public void detach() {
    }

}

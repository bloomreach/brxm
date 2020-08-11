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
package org.onehippo.cms7.services.htmlprocessor.richtext.model;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorFactory;
import org.onehippo.cms7.services.htmlprocessor.TagVisitor;
import org.onehippo.cms7.services.htmlprocessor.model.HtmlProcessorModel;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.URLEncoder;
import org.onehippo.cms7.services.htmlprocessor.richtext.URLProvider;
import org.onehippo.cms7.services.htmlprocessor.richtext.image.RichTextImageFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.image.RichTextImageFactoryImpl;
import org.onehippo.cms7.services.htmlprocessor.richtext.image.RichTextImageURLProvider;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.NodeFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.link.RichTextLinkFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.link.RichTextLinkFactoryImpl;
import org.onehippo.cms7.services.htmlprocessor.richtext.visit.ImageAndLinkVisitor;

public class RichTextProcessorModel extends HtmlProcessorModel {

    private final List<TagVisitor> visitors = new ArrayList<>();

    public RichTextProcessorModel(final Model<String> valueModel, final Model<Node> nodeModel,
                                  final HtmlProcessorFactory processorFactory, final NodeFactory nodeFactory,
                                  final URLEncoder encoder) {
        super(valueModel, processorFactory);

        final RichTextLinkFactory linkFactory = new RichTextLinkFactoryImpl(nodeModel, nodeFactory);
        final RichTextImageFactory imageFactory = new RichTextImageFactoryImpl(nodeModel, nodeFactory, encoder);
        final URLProvider imageProvider = createImageURLProvider(nodeModel, linkFactory, imageFactory);
        visitors.add(new ImageAndLinkVisitor(nodeModel, imageProvider));
    }

    protected URLProvider createImageURLProvider(final Model<Node> nodeModel, final RichTextLinkFactory linkFactory, final RichTextImageFactory richTextImageFactory) {
        return new RichTextImageURLProvider(richTextImageFactory, linkFactory, nodeModel);
    }

    @Override
    public List<TagVisitor> getVisitors() {
        return visitors;
    }

}

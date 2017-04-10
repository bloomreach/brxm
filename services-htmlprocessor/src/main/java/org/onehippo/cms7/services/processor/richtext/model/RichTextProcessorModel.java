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
package org.onehippo.cms7.services.processor.richtext.model;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.onehippo.cms7.services.processor.html.HtmlProcessorFactory;
import org.onehippo.cms7.services.processor.html.model.HtmlProcessorModel;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.html.visit.TagVisitor;
import org.onehippo.cms7.services.processor.richtext.URLProvider;
import org.onehippo.cms7.services.processor.richtext.jcr.NodeFactory;
import org.onehippo.cms7.services.processor.richtext.image.RichTextImageFactory;
import org.onehippo.cms7.services.processor.richtext.image.RichTextImageURLProvider;
import org.onehippo.cms7.services.processor.richtext.link.RichTextLinkFactory;
import org.onehippo.cms7.services.processor.richtext.URLEncoder;
import org.onehippo.cms7.services.processor.richtext.image.RichTextImageFactoryImpl;
import org.onehippo.cms7.services.processor.richtext.link.RichTextLinkFactoryImpl;
import org.onehippo.cms7.services.processor.richtext.visit.ImageVisitor;
import org.onehippo.cms7.services.processor.richtext.visit.LinkVisitor;

public class RichTextProcessorModel extends HtmlProcessorModel {

    private final Model<Node> nodeModel;
    private final List<TagVisitor> visitors = new ArrayList<>();

    public RichTextProcessorModel(final Model<String> valueModel, final Model<Node> nodeModel,
                                  final HtmlProcessorFactory processorFactory, final NodeFactory nodeFactory) {
        this(valueModel, nodeModel, processorFactory, nodeFactory, URLEncoder.OPAQUE);
    }

    public RichTextProcessorModel(final Model<String> valueModel, final Model<Node> nodeModel,
                                  final HtmlProcessorFactory processorFactory, final NodeFactory nodeFactory,
                                  final URLEncoder encoder) {
        super(valueModel, processorFactory);

        this.nodeModel = nodeModel;

        final RichTextLinkFactory linkFactory = new RichTextLinkFactoryImpl(nodeModel, nodeFactory);
        final RichTextImageFactory richTextImageFactory = new RichTextImageFactoryImpl(nodeModel, nodeFactory, encoder);
        final URLProvider imageProvider = createRichTextImageURLProvider(nodeModel, linkFactory,
                                                                         richTextImageFactory);
        visitors.add(new LinkVisitor(nodeModel));
        visitors.add(new ImageVisitor(nodeModel, imageProvider));
    }

    protected URLProvider createRichTextImageURLProvider(final Model<Node> nodeModel, final RichTextLinkFactory linkFactory, final RichTextImageFactory richTextImageFactory) {
        return new RichTextImageURLProvider(richTextImageFactory, linkFactory, nodeModel);
    }

    @Override
    protected List<TagVisitor> getVisitors() {
        return visitors;
    }

    @Override
    public void release() {
        super.release();
        if (nodeModel != null) {
            nodeModel.release();
        }
        visitors.forEach(TagVisitor::release);
    }
}

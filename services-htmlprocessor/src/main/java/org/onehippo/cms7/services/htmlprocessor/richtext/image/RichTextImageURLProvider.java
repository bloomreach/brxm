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
package org.onehippo.cms7.services.htmlprocessor.richtext.image;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.util.FacetUtil;
import org.onehippo.cms7.services.htmlprocessor.richtext.URLProvider;
import org.onehippo.cms7.services.htmlprocessor.richtext.RichTextException;
import org.onehippo.cms7.services.htmlprocessor.richtext.link.RichTextLinkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.services.htmlprocessor.util.JcrUtil.PATH_SEPARATOR;

public class RichTextImageURLProvider implements URLProvider {

    public static final Logger log = LoggerFactory.getLogger(RichTextImageURLProvider.class);

    private static final String IMAGE_NOT_FOUND = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA/BJREFUeNrsV99Pk1cYfkpbfhQKCqxxoMZEg8WhFsUL55jX6mSwdGoMf8CWaExcvPBv8Md0M9EsXpgs8UfWZM7NceONSGHRXShkxDIwGBLSsZYA2h9Qin7PO9/6tbQfd+sNJzk553w933me932f9z1fbaFQ6CcAfvz/LdDU1PQlDAJvitHe4aIERW6rBIpOwGFe9PT0YH5+HktLS3A6naiqqoLD8X6LzWaTbp7rWqwpKVn2W+5+n89XmEA8Hkd5ebkcVFZWBpfLhfr6ehlJyurgfF1/13F8fNw6BHNzc2I5ARcXF4XQ5uYtcAT7YLfbhZgVkHZHfx/WeNYu85zZW3kJzM7OorS0VMDT6bRY/ez8JXxw/AhK+nozJJRI7qjg7q4OxO7+ZgmclwAPSqVSAs6RLdqyHU/PfQvP8aMoHejPAs3tzv6ggMd/uY/0J+0FvVWQAOMuVhjCq6iokFAwJLO+Voxdu44afyfK/hjI6wWCV3UdzgLPJbsiAVrOxhDQ3dQAyyX7P95mTN28A/cXHeIJMwDXlZ2fIX7vPpbaP13mIbVes6QgAQKzEVAJ6bNkMolwk1diS0uVhHMgCNfnh5C49zvetO/PC2wma1kHGPdEIiFul4tC3Wus6+rqUFtbi/SmTeJmgtJiV8d/4Gp5buqtlAVZBOh2kiAw57LB0APnlZWVmXrAGCt48tcew3ID3AJY5yuGgFay+rEtLCygpqZGCpFWQw2NPfhIwEmi/PAB2HofZv3OkYBC1ggj5wzl8PCwtQf4AoVHQPbq6mo0NjZmRMmDWZTo/pnAz0j6diFxJ4C6joOyTu3dJ2DcNzU1Jbqh5/juxMQExsbGrAmEw2Gxmi+QeTQaRSwWk5R0u91w/fkY1Uf9CP94C/GWHUi/egX7zlakjPU6fxeitwNY2PsxpqenMTo6mpXabAyhJQF1Hy8kMmdpnpyclPvhw5EQ2s6ewcvrN/Dv5i1IGs9JkgLFho2Y+f4qvMf8eG6M0eZtQth8OdG7qquCGmhoaBAPUAcEJXuPx4P1L0YF/K9LVxDe6hVghoh71LrIRy0YvHAZ3pNfY82zpxlwGkQ9UdwkYUlAD6VV7JyvCz1H65nTCF25hte72zKFySw6Vftc6y4MXfwOO745hdqhwcwec02wJKD3ABlz5MtbT3yFv6/+gFjbHjmAz7WryilSvsNxZqcPgxcvCwlzOutFZqkBHsDOG5HCI8CT4ONlZVnTTDMn14jIthb0PniY2UPPcp5PA1kECEzRUQeRSETSiNWPIzOCpBSU1nDUDxUNh5mUAmsYzF9XeQl0d3evfhWvEijK/4LAyMhIUf4dSxEzp08x2lsBBgDZEGbyoGN6VAAAAABJRU5ErkJggg==";

    private final RichTextImageFactory imageFactory;
    private final RichTextLinkFactory linkFactory;
    private final Model<Node> nodeModel;

    public RichTextImageURLProvider(final RichTextImageFactory factory, final RichTextLinkFactory linkFactory,
                                    final Model<Node> nodeModel) {
        this.imageFactory = factory;
        this.linkFactory = linkFactory;
        this.nodeModel = nodeModel;
    }

    @Override
    public String getURL(final String link) {
        try {
            final Node node = this.nodeModel.get();
            final String facetName = StringUtils.substringBefore(link, PATH_SEPARATOR);
            final String type = StringUtils.substringAfterLast(link, PATH_SEPARATOR);
            final String uuid = FacetUtil.getChildDocBaseOrNull(node, facetName);
            if (uuid != null && linkFactory.hasLink(uuid)) {
                final RichTextImage rti = imageFactory.loadImageItem(uuid, type);
                return rti.getUrl();
            }
            return link;
        } catch (final RichTextException e) {
            log.error("Error creating image link for input '{}'", link, e);
            return IMAGE_NOT_FOUND;
        }
    }
}

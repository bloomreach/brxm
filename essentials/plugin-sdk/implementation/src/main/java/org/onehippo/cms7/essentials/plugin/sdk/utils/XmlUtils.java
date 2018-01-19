/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.onehippo.cms7.essentials.plugin.sdk.utils.xml.XmlNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XmlUtils is used for manipulating XML files on a file system
 *
 * @version "$Id$"
 */
public final class XmlUtils {

    private static Logger log = LoggerFactory.getLogger(XmlUtils.class);

    private XmlUtils() {
    }

    public static XmlNode parseXml(final InputStream content) {

        try {
            final JAXBContext context = JAXBContext.newInstance(XmlNode.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            return (XmlNode) unmarshaller.unmarshal(content);
        } catch (JAXBException e) {
            if (log.isDebugEnabled()) {
                log.error("Error parsing XmlNode document", e.getMessage());
            }
        }

        return null;
    }
}

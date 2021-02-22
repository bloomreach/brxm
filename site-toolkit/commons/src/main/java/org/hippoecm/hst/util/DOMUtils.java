/*
 *  Copyright 2018-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

/**
 * XmlDomUtils
 */
public class DOMUtils {

    private DOMUtils() {
    }

    public static Comment createComment(String comment) throws DOMException {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        try {
            dbfac.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            return doc.createComment(comment);
        } catch (ParserConfigurationException e) {
            throw new DOMException((short) 0, "Initialization failure");
        }
    }

}

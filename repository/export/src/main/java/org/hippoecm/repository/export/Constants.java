/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.repository.export;

import org.dom4j.Namespace;
import org.dom4j.QName;

final class Constants {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ";

    private Constants() {
    }
    static final Namespace JCR_NAMESPACE = new Namespace("sv", "http://www.jcp.org/jcr/sv/1.0");
    static final QName NAME_QNAME = new QName("name", JCR_NAMESPACE);
    static final QName TYPE_QNAME = new QName("type", JCR_NAMESPACE);
    static final QName NODE_QNAME = new QName("node", JCR_NAMESPACE);
    static final QName PROPERTY_QNAME = new QName("property", JCR_NAMESPACE);
    static final QName VALUE_QNAME = new QName("value", JCR_NAMESPACE);
}

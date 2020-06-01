/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.jaxrs.model;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.demo.beans.TextBean;

/**
 * @version $Id$
 */
@XmlRootElement(name = "document")
public class TextDocumentRepresentation extends BaseDocumentRepresentation {
    
    public TextDocumentRepresentation represent(TextBean bean) throws RepositoryException {
        super.represent(bean);
        return this;
    }
    
}

/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.model.contentblocks;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.rest.model.Restful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "cbpayload")
public class CBPayload implements Restful {

    private static final long serialVersionUID = 1L;

    private RestfulList<DocumentTypes> items;

    @XmlElement(name = "items")
    public RestfulList<DocumentTypes> getItems() {
        return items;
    }

    public void setItems(final RestfulList<DocumentTypes> items) {
        this.items = items;
    }
}

/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.contentblocks.model.contentblocks;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.model.Restful;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.RestList;


/**
 * @version "$Id$"
 */
@XmlRootElement(name = "documentType")
public class DocumentType extends KeyValueRestful implements Restful {

    private static final long serialVersionUID = 1L;

    public DocumentType() {
    }

    private RestList<KeyValueRestful> providers;

    public DocumentType(final String key, final String value, final RestList<KeyValueRestful> providers) {
        super(key, value);
        this.providers = providers;
    }

    @XmlElement(name = "providers")
    public RestList<KeyValueRestful> getProviders() {
        return providers;
    }

    public void setProviders(final RestList<KeyValueRestful> providers) {
        this.providers = providers;
    }
}
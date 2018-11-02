/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.demo.example.commerce.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "errors")
public class ProductErrorsBody {

    private final int status;
    private final String message;

    public ProductErrorsBody() {
        this(0, null);
    }

    public ProductErrorsBody(final int status, final String message) {
        
        this.status = status;
        this.message = message;
    }

    @XmlElement
    public int getStatus() {
        return status;
    }

    @XmlElement
    public String getMessage() {
        return message;
    }

}

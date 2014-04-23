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

package org.onehippo.cms7.essentials.dashboard.utils.xml;

import java.io.Serializable;
import java.util.Collection;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @version "$Id: NodeOrProperty.java 172944 2013-08-06 16:37:37Z mmilicevic $"
 */
public interface NodeOrProperty extends Serializable {


    @XmlTransient
    String getType();

    @XmlTransient
    Collection<NodeOrProperty> getXmlNodeOrXmlProperty();

    String getName();

    boolean isNode();

    boolean isProperty();

    @XmlTransient
    Boolean getMultiple();

    XmlProperty getPropertyForName(String propertyName);
}

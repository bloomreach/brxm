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
package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import javax.jcr.PropertyType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateFieldType extends PrimitiveFieldType {

    private static final Logger log = LoggerFactory.getLogger(DateFieldType.class);
    private static final String DEFAULT_VALUE = StringUtils.EMPTY;

    public DateFieldType() {
        setType(Type.DATE);
    }

    @Override
    protected int getPropertyType() {
        return PropertyType.DATE;
    }

    @Override
    protected String getDefault() {
        return DEFAULT_VALUE;
    }
}

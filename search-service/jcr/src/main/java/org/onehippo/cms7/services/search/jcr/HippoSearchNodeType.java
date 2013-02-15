/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.jcr;

public interface HippoSearchNodeType {

    String NT_PRIMITIVECONSTRAINT = "hipposearch:primitiveconstraint";
    String NT_COMPOUNDCONSTRAINT = "hipposearch:compoundconstraint";

    String NT_QUERY = "hipposearch:query";

    // abstract hipposearch:constraint
    String NEGATE = "hipposearch:negate";

    // compound constraint
    String TYPE = "hipposearch:type";
    String CONSTRAINT = "hipposearch:constraint";

    // primitive constraint
    String RELATION = "hipposearch:relation";
    String PROPERTY = "hipposearch:property";
    String VALUE = "hipposearch:value";
    String UPPER = "hipposearch:upper";
    String RESOLUTION = "hipposearch:resolution";

    // query
    String LIMIT = "hipposearch:limit";
    String OFFSET = "hipposearch:offset";
    String NODETYPE = "hipposearch:nodetype";
    String ORDER_BY = "hipposearch:orderBy";
    String ASC_DESC = "hipposearch:ascDesc";
    String INCLUDES = "hipposearch:includes";
    String EXCLUDES = "hipposearch:excludes";
}

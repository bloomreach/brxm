/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.query.lucene;

public class ServicingFieldNames {
    
    /**
     * Private constructor.
     */
    private ServicingFieldNames() {
    }
    
    /**
     * Prefix for all field names that are facet properties.
     */
    public static final String HIPPO_FACET = "HIPPOFACET:".intern();

    /**
     * Prefix for all field names that are path properties.
     */
    public static final String HIPPO_PATH = "_:HIPPOPATH:".intern();
    

    /**
     * Prefix for all field names that are depth properties.
     */
    public static final String HIPPO_DEPTH = "_:HIPPODEPTH:".intern();
    
    /**
     * Name of the field that contains all available properties that are available
     * for this indexed node. 
     */
    public static final String FACET_PROPERTIES_SET = "_:FACET_PROPERTIES_SET".intern(); 
    
}

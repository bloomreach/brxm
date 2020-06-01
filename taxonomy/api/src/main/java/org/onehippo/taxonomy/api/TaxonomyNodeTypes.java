/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.api;

public interface TaxonomyNodeTypes {

    String NODETYPE_HIPPOTAXONOMY_CONTAINER = "hippotaxonomy:container";
    String NODETYPE_HIPPOTAXONOMY_TAXONOMY = "hippotaxonomy:taxonomy";
    String NODETYPE_HIPPOTAXONOMY_CATEGORY = "hippotaxonomy:category";
    String NODETYPE_HIPPOTAXONOMY_CLASSIFIABLE = "hippotaxonomy:classifiable";
    String NODETYPE_HIPPOTAXONOMY_CANONISED = "hippotaxonomy:canonised";
    
    String HIPPOTAXONOMY_CATEGORYINFOS = "hippotaxonomy:categoryinfos";
    String HIPPOTAXONOMY_CATEGORYINFO = "hippotaxonomy:categoryinfo";
    
    String HIPPOTAXONOMY_LOCALES = "hippotaxonomy:locales";
    
    String HIPPOTAXONOMY_NAME = "hippotaxonomy:name";
    String HIPPOTAXONOMY_DESCRIPTION = "hippotaxonomy:description";
    String HIPPOTAXONOMY_SYNONYMS = "hippotaxonomy:synonyms";

    String HIPPOTAXONOMY_KEY = "hippotaxonomy:key";
    String HIPPOTAXONOMY_KEYS = "hippotaxonomy:keys";
    String HIPPOTAXONOMY_CANONICALKEY = "hippotaxonomy:canonkey";
    
}

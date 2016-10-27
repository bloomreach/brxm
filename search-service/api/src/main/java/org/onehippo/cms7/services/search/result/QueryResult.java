/*
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.result;

/**
 * Wrapper interface of response from search service.
 */
public interface QueryResult {

    QueryResult EMPTY = new QueryResult() {

        @Override
        public HitIterator getHits() {
            return HitIterator.EMPTY;
        }

        @Override
        public long getTotalHitCount() {
            return 0;
        }

    };

    /**
     * Returns returned content item list
     * @return
     */
    HitIterator getHits();

    /**
     * Returns the total count of hits
     * @return the total count of hits
     */
    long getTotalHitCount();


}

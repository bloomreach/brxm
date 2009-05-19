/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.content.beans.query.filter;

import org.hippoecm.hst.content.beans.query.exceptions.FilterException;


public interface Filter extends BaseFilter{

    void addContains(String scope, String fullTextSearch) throws FilterException ;

    void addBetween(String fieldAttributeName, Object value1, Object value2) throws FilterException ;

    void addEqualTo(String fieldAttributeName, Object value) throws FilterException ;

    void addGreaterOrEqualThan(String fieldAttributeName, Object value) throws FilterException ;

    void addGreaterThan(String fieldAttributeName, Object value) throws FilterException ;

    void addLessOrEqualThan(String fieldAttributeName, Object value) throws FilterException ;

    void addLessThan(String fieldAttributeName, Object value) throws FilterException ;

    /**
     * Try to avoid this method, as 'like' searches are slow. Certainly try to avoid prefix wildcards in the
     * value, as they do not scale at all.
     * @param fieldAttributeName
     * @param value
     * @throws FilterException
     */
    void addLike(String fieldAttributeName, Object value) throws FilterException ;

    void addNotEqualTo(String fieldAttributeName, Object value) throws FilterException ;

    void addNotNull(String fieldAttributeName) throws FilterException ;

    void addIsNull(String fieldAttributeName) throws FilterException ;
    
    void addJCRExpression(String jcrExpression);
    
    Filter addOrFilter(BaseFilter filter);

    Filter addAndFilter(BaseFilter filter);

}

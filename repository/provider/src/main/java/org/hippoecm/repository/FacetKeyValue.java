/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;



public class FacetKeyValue implements KeyValue<String, String> {

    private String key;
    private String value;
    public FacetKeyValue(String key, String value) {
        this.key = key;
        this.value = value;
    }
    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
    
    @Override 
    public String toString(){
        return key + "=" + value;
    }
    
    @Override 
    public int hashCode(){
        return (key==null   ? 0 : key.hashCode()) ^
        (value==null ? 0 : value.hashCode());
    }
    
    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof FacetKeyValue)) {
            return false;
        }
        FacetKeyValue e = (FacetKeyValue)o;
        Object k1 = getKey();
        Object k2 = e.getKey();
        if (k1 == k2 || (k1 != null && k1.equals(k2))) {
            Object v1 = getValue();
            Object v2 = e.getValue();
            if (v1 == v2 || (v1 != null && v1.equals(v2)))
                return true;
        }
        return false;
    }

}

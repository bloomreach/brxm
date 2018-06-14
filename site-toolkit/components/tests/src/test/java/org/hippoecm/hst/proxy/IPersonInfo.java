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
package org.hippoecm.hst.proxy;

import java.util.Map;

public interface IPersonInfo {

    public void setFirstName(String firstName);
    
    public String getFirstName();
    
    public void setLastName(String lastName);
    
    public String getLastName();
    
    public void setAddresses(Map<String, String> addresses);
    
    public Map<String, String> getAddresses();
    
    public void setAddress(String name, String value);
    
    public String getAddress(String name);
    
    public void setFavorites(String [] favorites);
    
    public String [] getFavorites();
    
    public void setFavorite(int index, String favorite);
    
    public String getFavorite(int index);
    
    public String toXMLString();
    
}

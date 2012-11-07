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
package org.hippoecm.hst.demo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoItem;

@Node(jcrType = "demosite:placecompound")
public class PlaceBean extends HippoItem {
    private String city;
    private String country;

    public String getCity() {
        return city == null ? (String) getProperty("demosite:city") : city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country == null ? (String) getProperty("demosite:country") : country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}

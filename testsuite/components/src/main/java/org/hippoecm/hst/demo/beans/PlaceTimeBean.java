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
package org.hippoecm.hst.demo.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoItem;

@Node(jcrType = "demosite:placetimecompound")
public class PlaceTimeBean extends HippoItem {
    private Calendar date;

    public Calendar getDate() {
        return date == null ? (Calendar) getProperty("demosite:date") : date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public PlaceBean getPlace() {
        return getBean("demosite:demosite_placecompound", PlaceBean.class);
    }
}

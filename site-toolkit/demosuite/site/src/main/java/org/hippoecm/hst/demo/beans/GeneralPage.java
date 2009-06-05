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

import java.util.Calendar;

import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

public class GeneralPage extends HippoDocument {

    
    public String getTitle() {
        return getProperty("demosite:title");
    }
    
    public String getSummary() {
        return getProperty("demosite:summary");
    }

    public HippoHtml getHtml(){
        return getHippoHtml("demosite:body");    
    }
    
    /*
     * to be overridden by beans having a date. By having it in the generalpage as well, 
     * the jsp el can always try a var.date without getting an expression language exception
     */  
    public Calendar getDate() {
        return null;
    }
}

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
package org.hippoecm.faceteddate;

import java.util.Calendar;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class DerivedDataFunction extends org.hippoecm.repository.ext.DerivedDataFunction {

    public Map<String,Value[]> compute(Map<String,Value[]> parameters) {
        if(!parameters.containsKey("date")) {
            parameters.clear();
            return parameters;
        }
        try {
            Calendar date = parameters.get("date")[0].getDate();
            parameters.put("year", new Value[] { getValueFactory().createValue(date.get(Calendar.YEAR)) });
            parameters.put("month", new Value[] { getValueFactory().createValue(date.get(Calendar.MONTH)) });
            parameters.put("weekofyear", new Value[] { getValueFactory().createValue(date.get(Calendar.WEEK_OF_YEAR)) });
            parameters.put("dayofweek", new Value[] { getValueFactory().createValue(date.get(Calendar.DAY_OF_WEEK)) });
            parameters.put("dayofmonth", new Value[] { getValueFactory().createValue(date.get(Calendar.DAY_OF_MONTH)) });
            parameters.put("dayofyear", new Value[] { getValueFactory().createValue(date.get(Calendar.DAY_OF_YEAR)) });
            parameters.put("hourofday", new Value[] { getValueFactory().createValue(date.get(Calendar.HOUR_OF_DAY)) });
            parameters.put("minute", new Value[] { getValueFactory().createValue(date.get(Calendar.MINUTE)) });
            parameters.put("second", new Value[] { getValueFactory().createValue(date.get(Calendar.SECOND)) });
        } catch(ValueFormatException ex) {
            parameters.clear();
        } catch(RepositoryException ex) {
            parameters.clear();
        }
        return parameters;
    }
}

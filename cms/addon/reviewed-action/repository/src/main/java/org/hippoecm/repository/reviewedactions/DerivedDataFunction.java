/*
 * Copyright 2007 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Map;

import javax.jcr.Value;
import javax.jcr.ValueFactory;

public class DerivedDataFunction extends org.hippoecm.repository.ext.DerivedDataFunction {
    public Map<String,Value[]> compute(Map<String,Value[]> parameters) {
        if(parameters.containsKey("draft")) {
            parameters.put("summary", new Value[] { getValueFactory().createValue("modified") });
        } else if(parameters.containsKey("unpublished")) {
            parameters.put("summary", new Value[] { getValueFactory().createValue("preview") });
        } else if(parameters.containsKey("published")) {
            parameters.put("summary", new Value[] { getValueFactory().createValue("live") });
        } else {
            parameters.put("summary", new Value[] { getValueFactory().createValue("new") });
        }
        return parameters;
    }
}

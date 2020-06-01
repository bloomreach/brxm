/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.deriveddata;

import java.util.Map;

import javax.jcr.Value;

import org.hippoecm.repository.ext.DerivedDataFunction;

public class CopyDerivedDataFunction extends DerivedDataFunction {
    private static final long serialVersionUID = 1;

    @Override
    public Map<String, Value[]> compute(Map<String, Value[]> parameters) {
        Value[] source = parameters.get("source");
        if (source != null) {
            Value[] destination = new Value[source.length];
            System.arraycopy(source, 0, destination, 0, source.length);
            parameters.put("destination", destination);
        }
        return parameters;
    }
}

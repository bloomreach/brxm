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

package org.hippoecm.frontend.plugins.yui.util;

import java.util.regex.Pattern;

//TODO: remove
@Deprecated
public class JavascriptUtil {

    public static final String SINGLE_QUOTE = "'";

    private static Pattern numbers = Pattern.compile("\\d*");
    
    @Deprecated
    public static String serialize2JS(String value) {
        if (value == null) {
            return "null";
        } else if(value.equals("")) {
            return SINGLE_QUOTE + SINGLE_QUOTE;
        } else if (value.equalsIgnoreCase("true")) {
            return "true";
        } else if (value.equalsIgnoreCase("false")) {
            return "false";
        } else if (numbers.matcher(value).matches()) { // || functions.matcher(value).find())
            return value;
        }
        return SINGLE_QUOTE + value.replaceAll(SINGLE_QUOTE, "\\\\" + SINGLE_QUOTE) + SINGLE_QUOTE;
    }

}

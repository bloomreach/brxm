/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PatternSet {

    private List<Pattern> patterns;
    private PatternSet parentPatternSet;

    PatternSet(List<String> patterns) {
        this(null, patterns);
    }
    
    PatternSet(PatternSet parentPatternSet, List<String> patterns) {
        this.patterns = new ArrayList<>();
        for (String pattern : patterns) {
            this.patterns.add(Pattern.compile(compile(pattern)));
        }
        this.parentPatternSet = parentPatternSet;
    }

    static String compile(String s) {
        // replace ** with .*
        StringBuilder sb = new StringBuilder(s.length());
        int offset = 0;
        int index;
        while ((index = s.indexOf("**", offset)) != -1) {
            sb.append(s.substring(offset, index));
            sb.append(".*");
            offset = index + 2;
        }
        if (offset < s.length()) {
            sb.append(s.substring(offset));
        }
        s = sb.toString();
        // replace * with [^/]*
        sb = new StringBuilder(s.length());
        offset = 0;
        while((index = s.indexOf('*', offset)) != -1) {
            if (s.charAt(index-1) != '.') {
                sb.append(s.substring(offset, index));
                sb.append("[^/]*");
            } else {
                sb.append(s.substring(offset, index+1));
            }
            offset = index + 1;
        }
        if (offset < s.length()) {
            sb.append(s.substring(offset));
        }
        return sb.toString();
    }

    public boolean matches(String s) {
        if (parentPatternSet != null && parentPatternSet.matches(s)) {
            return true;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(s).matches()) {
                return true;
            }
        }
        return false;
    }

}

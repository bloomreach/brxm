/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.api;

import org.hippoecm.repository.api.NodeNameCodec;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class KeyCodec {

    public static String encode(String input) {
        String cleaned = input.replaceAll(" ","-");
        cleaned = cleaned.replaceAll("&","-");
        cleaned = cleaned.replaceAll("=","-");
        cleaned = deAccent(cleaned);
        return NodeNameCodec.encode(cleaned, true);
    }

    public static String deAccent(String str) {
        String nfdNormalizedString = Normalizer.normalize(str.toLowerCase(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("");
    }

    public static String decode(String input) {
        return NodeNameCodec.decode(input);
    }

}

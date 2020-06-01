/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.net.URLEncoder;

import org.hippoecm.hst.util.EncodingUtils;
import org.junit.Test;

public class TestIsoLatin1AccentReplacer {
    
    @Test
    public void testUnAccentedInput() throws Exception {
        
        
        String input = "aaabbbcccdddee";
        String output = EncodingUtils.foldToASCIIReplacer(input);
        assertEquals("output is expected to be same as input but was not" , input, output);
        
    }
    
    @Test
    public void testAccentedInput() throws Exception {
        
        // iso latin 1 accented string
        String input = "\u00EB\u00E7\u00E9\u00DF";
        // the encoded version of the input is different
        String utf8EncodedInput = URLEncoder.encode(input, "UTF-8");
        assertNotSame("utf8EncodedInput is expected to be different then input because of accents", utf8EncodedInput ,input);
        
        String output = EncodingUtils.foldToASCIIReplacer(input);
        // if all accented chars are replaced correctly, then the utf-8 encoded version of the output is the same as the output
        String utf8EncodedOutput = URLEncoder.encode(output, "UTF-8");
        assertEquals("output is expected to have no accents but was '"+output+"'", utf8EncodedOutput, output);
        
    }
    
    /**
     * Now we check some chinese chars
     * @throws Exception
     */
    @Test
    public void testNonLatinInput() throws Exception {
        
        // two chinese chars
        String input = "\u53F0\u5317";
        String output = EncodingUtils.foldToASCIIReplacer(input);
        
        // the iso latin 1 accent replacer cannot replace chinese chars 
        assertEquals("output is expected to be same as input because cannot replace chinese chars", input , output);
        String utf8EncodedOutput = URLEncoder.encode(output, "UTF-8");
        assertNotSame("Encoded output is expect to be different because of chinese chars ", utf8EncodedOutput, output);
        
    }

}

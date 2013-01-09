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
package org.hippoecm.hst.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


public class TestSearchInputParsingUtils {
    
    @Test
    public void compressWhitespace_compressesWhitespace() {
        assertEquals("tabs are replaced with single space", ". .", SearchInputParsingUtils.compressWhitespace(".\t."));
        assertEquals("newlines are replaced with single space", ". .", SearchInputParsingUtils.compressWhitespace(".\n."));
        assertEquals("multiple spaces are replaced with single space", ". .", SearchInputParsingUtils.compressWhitespace(".    ."));
        assertEquals("leading and trailing whitespace is trimmed", ".", SearchInputParsingUtils.compressWhitespace(" . "));
        assertEquals("a b c d", SearchInputParsingUtils.compressWhitespace(" \t a \n b    c          d  "));
        assertEquals("is nullsafe", null, SearchInputParsingUtils.compressWhitespace(null));
    }
    
    @Test
    public void testNullArgument() throws Exception {
         assertNull(SearchInputParsingUtils.parse(null, true));
    }
    @Test
    public void testSearchInputParsingUtils_parse() throws Exception {
        assertEquals("The quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The quick brown fox jumps over the lazy dog", true));
        assertEquals("The qui*ck brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The qui*ck brown fox jumps over the lazy dog", true));
        assertEquals("The qui?ck brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The qui?ck brown fox jumps over the lazy dog", true));
        assertEquals("The quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The *quick brown fox jumps over the lazy dog", true));
        assertEquals("The quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The ?quick brown fox jumps over the lazy dog", true));

        assertEquals("Me and the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("Me and the quick brown fox jumps over the lazy dog", true));
        assertEquals("Me the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("Me AND the quick brown fox jumps over the lazy dog", true));
        assertEquals("Me or the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("Me or the quick brown fox jumps over the lazy dog", true));
        assertEquals("Me OR the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("Me OR the quick brown fox jumps over the lazy dog", true));
        
        assertEquals("the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("AND the quick brown fox jumps over the lazy dog", true));
        assertEquals("and the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("and the quick brown fox jumps over the lazy dog", true));
        assertEquals("or the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("or the quick brown fox jumps over the lazy dog", true));
        assertEquals("the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("OR the quick brown fox jumps over the lazy dog", true));
        
        assertEquals("AND* the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("AND* the quick brown fox jumps over the lazy dog", true));
        assertEquals("the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("*AND the quick brown fox jumps over the lazy dog", true));
        assertEquals("OR* the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("OR* the quick brown fox jumps over the lazy dog", true));
        assertEquals("the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("*OR the quick brown fox jumps over the lazy dog", true));
        
        assertEquals("the quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("*OR *the *?quick *?brown fox jumps over the lazy dog", true));
        assertEquals("the qu?ick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("*OR *the *?qu?ick *?brown fox jumps over the lazy dog", true));
        // only the first wildcard within a word is allowed
        assertEquals("The qu?ick br?own fo*x jumps over the lazy dog", SearchInputParsingUtils.parse("The ?*qu??ic?k br?ow*?n fo*x jumps over the lazy dog", true));
        // allow wildcard false
        assertEquals("The quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The ?*qu??ic?k br?ow*?n fo*x jumps over the lazy dog", false));
        
        assertEquals("The quick brown fox jumps  over   the lazy dog", SearchInputParsingUtils.parse("The (quick brown) (fox jumps) &( over ] ] [the lazy dog", true));
        
        assertEquals("The -quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The NOT quick brown fox jumps over the lazy dog", true));
        
        assertEquals("The quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The \t\n quick brown fox \njumps \t over the lazy dog", true));
        
        // prefix ~ is allowed for synonyms Jackrabbit. In middle of word, it is not allowed
        assertEquals("The ~quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The ~quick brown fox jumps over the lazy dog", true));
        assertEquals("The ~quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The ~qui~ck~ bro~wn fox jumps over the lazy dog", true));
        
    }

    @Test
    public void testSearchInputParsingUtils_removeLeadingWildCardsFromWords() throws Exception {
        assertEquals("The quick brown fox jumps over the lazy dog", SearchInputParsingUtils.removeLeadingWildCardsFromWords("The quick brown fox jumps over the lazy dog"));
        assertEquals("The qui*ck brown fox jumps over the lazy dog", SearchInputParsingUtils.removeLeadingWildCardsFromWords("The qui*ck brown fox jumps over the lazy dog"));
        assertEquals("The qui*ck brown* fox jumps* over the lazy dog", SearchInputParsingUtils.removeLeadingWildCardsFromWords("The qui*ck brown* fox jumps* over the lazy dog"));
        assertEquals("The qui*ck brown* fox jumps* over the lazy dog", SearchInputParsingUtils.removeLeadingWildCardsFromWords("The *qui*ck *brown* fox jumps* over the lazy dog"));
        
    }

}

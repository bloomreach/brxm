/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.jcr.query;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertNull;


public class TestFullTextSearchParser {
    
    @Test
    public void compressWhitespace_compressesWhitespace() {
        assertEquals("tabs are replaced with single space", ". .", FullTextSearchParser.compressWhitespace(".\t."));
        assertEquals("newlines are replaced with single space", ". .", FullTextSearchParser.compressWhitespace(".\n."));
        assertEquals("multiple spaces are replaced with single space", ". .", FullTextSearchParser.compressWhitespace(".    ."));
        assertEquals("leading and trailing whitespace is trimmed", ".", FullTextSearchParser.compressWhitespace(" . "));
        assertEquals("a b c d", FullTextSearchParser.compressWhitespace(" \t a \n b    c          d  "));
        assertEquals("is nullsafe", null, FullTextSearchParser.compressWhitespace(null));
    }
    
    @Test
    public void testNullArgument() throws Exception {
         assertNull(FullTextSearchParser.fullTextParseHstMode(null, true));
    }

    @Test
    public void testCmsSimpleSearchModeeParsing() throws Exception {
        assertEquals("The quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The quick brown fox jumps over the lazy dog", false));
        assertEquals("The* quick* brown* fox* jumps* over* the* lazy* dog*",
                FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The quick brown fox jumps over the lazy dog", true));

        assertEquals("The quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The ?*qu??ic?k br?ow*?n fo*x jumps over the lazy dog", false));
        assertEquals("The* quick* brown* fox* jumps* over* the* lazy* dog*",
                FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The ?*qu??ic?k br?ow*?n fo*x jumps over the lazy dog", true));


        assertEquals("CMS-345",FullTextSearchParser.fullTextParseCmsSimpleSearchMode("CMS-345", false));
        assertEquals("CMS-345*",FullTextSearchParser.fullTextParseCmsSimpleSearchMode("CMS-345", true));

        assertEquals("The quick",FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The !qui!ck!", false));
        assertEquals("The -quick",FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The -quick-", false));
        assertEquals("The* quick*",FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The !quick!", true));
        assertEquals("The* -quick*",FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The -quick-", true));
        assertEquals("The* quick*",FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The !qui!ck!", true));
        assertEquals("The* -qui-ck*",FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The -qui-ck-", true));
        assertEquals("The* -quick*",FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The -quick -*", true));
        assertEquals("The -quick",FullTextSearchParser.fullTextParseCmsSimpleSearchMode("The -quick -*", false));
    }


    @Test
    public void testHstModeParsing() throws Exception {
        assertEquals("The quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The quick brown fox jumps over the lazy dog", true));
        assertEquals("The qui*ck brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The qui*ck brown fox jumps over the lazy dog", true));
        assertEquals("The qui?ck brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The qui?ck brown fox jumps over the lazy dog", true));
        assertEquals("The quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The *quick brown fox jumps over the lazy dog", true));
        assertEquals("The quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The ?quick brown fox jumps over the lazy dog", true));

        assertEquals("Me and the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("Me and the quick brown fox jumps over the lazy dog", true));
        assertEquals("Me the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("Me AND the quick brown fox jumps over the lazy dog", true));
        assertEquals("Me or the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("Me or the quick brown fox jumps over the lazy dog", true));
        assertEquals("Me OR the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("Me OR the quick brown fox jumps over the lazy dog", true));
        
        assertEquals("the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("AND the quick brown fox jumps over the lazy dog", true));
        assertEquals("and the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("and the quick brown fox jumps over the lazy dog", true));
        assertEquals("or the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("or the quick brown fox jumps over the lazy dog", true));
        assertEquals("the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("OR the quick brown fox jumps over the lazy dog", true));
        
        assertEquals("AND* the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("AND* the quick brown fox jumps over the lazy dog", true));
        assertEquals("the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("*AND the quick brown fox jumps over the lazy dog", true));
        assertEquals("OR* the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("OR* the quick brown fox jumps over the lazy dog", true));
        assertEquals("the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("*OR the quick brown fox jumps over the lazy dog", true));
        
        assertEquals("the quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("*OR *the *?quick *?brown fox jumps over the lazy dog", true));
        assertEquals("the qu?ick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("*OR *the *?qu?ick *?brown fox jumps over the lazy dog", true));
        // only the first wildcard within a word is allowed
        assertEquals("The qu?ick br?own fo*x jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The ?*qu??ic?k br?ow*?n fo*x jumps over the lazy dog", true));
        // allow wildcard false
        assertEquals("The quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The ?*qu??ic?k br?ow*?n fo*x jumps over the lazy dog", false));
        
        assertEquals("The quick brown fox jumps  over   the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The (quick brown) (fox jumps) &( over ] ] [the lazy dog", true));
        
        assertEquals("The -quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The NOT quick brown fox jumps over the lazy dog", true));
        
        assertEquals("The quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The \t\n quick brown fox \njumps \t over the lazy dog", true));
        
        // prefix ~ is allowed for synonyms Jackrabbit. In middle of word, it is not allowed
        assertEquals("The ~quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The ~quick brown fox jumps over the lazy dog", true));
        assertEquals("The ~quick brown fox jumps over the lazy dog",
                FullTextSearchParser.fullTextParseHstMode("The ~qui~ck~ bro~wn fox jumps over the lazy dog", true));

        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!*", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!*", false));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!!", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!!!", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!!!", false));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!!!*", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!*!!", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!!!*", false));

        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The qu!ick!*!!", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The qu!ick!!!*", false));

        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!!!?", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!?!!", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick!!!?", false));


        assertEquals("The quick !brown", FullTextSearchParser.fullTextParseHstMode("The quick! !brown", false));
        assertEquals("The quick !brown", FullTextSearchParser.fullTextParseHstMode("The quick! !brown", true));
        assertEquals("The quick !brown", FullTextSearchParser.fullTextParseHstMode("The qui!ck! !bro!wn!", false));
        assertEquals("The quick !brown", FullTextSearchParser.fullTextParseHstMode("The qui!ck! !bro!wn!", true));
        assertEquals("The quick !brown", FullTextSearchParser.fullTextParseHstMode("The quick! !brown", false));
        assertEquals("The quick !brown", FullTextSearchParser.fullTextParseHstMode("The quick! !brown", true));
        assertEquals("The quick !brown", FullTextSearchParser.fullTextParseHstMode("The quick! !brown*", false));
        assertEquals("The quick !brown*", FullTextSearchParser.fullTextParseHstMode("The quick! !brown*", true));

        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick-", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick-*", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick--", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick---", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick---", false));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick---*", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick-*--", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick---*", false));

        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick---?", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick-?--", true));
        assertEquals("The qu-ick", FullTextSearchParser.fullTextParseHstMode("The qu-ick---?", false));
        assertEquals("The qu-ick", FullTextSearchParser.fullTextParseHstMode("The qu-ick-?--", true));
        assertEquals("The quick", FullTextSearchParser.fullTextParseHstMode("The quick---?", false));


        assertEquals("The quick -brown", FullTextSearchParser.fullTextParseHstMode("The quick- -brown", false));
        assertEquals("The quick -brown", FullTextSearchParser.fullTextParseHstMode("The quick- -brown", true));
        assertEquals("The quick -brown", FullTextSearchParser.fullTextParseHstMode("The quick- -brown", false));
        assertEquals("The quick -brown", FullTextSearchParser.fullTextParseHstMode("The quick- -brown", true));
        assertEquals("The quick -brown", FullTextSearchParser.fullTextParseHstMode("The quick- -brown*", false));
        assertEquals("The quick -brown*", FullTextSearchParser.fullTextParseHstMode("The quick- -brown*", true));
        assertEquals("The quick -bro-wn", FullTextSearchParser.fullTextParseHstMode("The quick- -bro-wn*", false));
        assertEquals("The quick -bro-wn*", FullTextSearchParser.fullTextParseHstMode("The quick- -bro-wn*", true));

        assertEquals("", FullTextSearchParser.fullTextParseHstMode("!", true));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("!", false));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("!!", true));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("!!!", true));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("!!!*", true));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("!!!*", false));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("!!*!", true));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("!!*!", false));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("-", true));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("-", false));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("--", true));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("---", true));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("---*", true));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("---*", false));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("--*-", true));
        assertEquals("", FullTextSearchParser.fullTextParseHstMode("--*-", false));

    }

}

/*
 *  Copyright 2011-2016 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


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

        assertEquals("The quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The (quick brown) (fox jumps) &( over ] ] [the lazy dog", true));

        assertEquals("The -quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The NOT quick brown fox jumps over the lazy dog", true));

        assertEquals("The quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The \t\n quick brown fox \njumps \t over the lazy dog", true));

        // prefix ~ is allowed for synonyms Jackrabbit. In middle of word, it is considered whitespace, when preserveWordBound == false it is stripped
        assertEquals("The ~quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The ~quick brown fox jumps over the lazy dog", true));
        assertEquals("The ~qui ck bro wn fox jumps over the lazy dog", SearchInputParsingUtils.parse("The ~qui~ck~ bro~wn fox jumps over the lazy dog", true));
        assertEquals("The ~quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The ~qui~ck~ bro~wn fox jumps over the lazy dog", true, false));

        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!*", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!*", false));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!!", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!!!", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!!!", false));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!!!*", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!*!!", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!!!*", false));

        assertEquals("The qu ick", SearchInputParsingUtils.parse("The qu!ick!*!!", true));
        assertEquals("The qu ick", SearchInputParsingUtils.parse("The qu!ick!!!*", false));
        assertEquals("The quick", SearchInputParsingUtils.parse("The qu!ick!!!*", false, false));

        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!!!?", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!?!!", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick!!!?", false));


        assertEquals("The quick !brown", SearchInputParsingUtils.parse("The quick! !brown", false));
        assertEquals("The quick !brown", SearchInputParsingUtils.parse("The quick! !brown", true));
        assertEquals("The qui ck !bro wn", SearchInputParsingUtils.parse("The qui!ck! !bro!wn!", false));
        assertEquals("The qui ck !bro wn", SearchInputParsingUtils.parse("The qui!ck! !bro!wn!", true));
        assertEquals("The quick !brown", SearchInputParsingUtils.parse("The qui!ck! !bro!wn!", true, false));
        assertEquals("The qui ck !bro wn", SearchInputParsingUtils.parse("The qui!ck! !bro!wn!", true, true));
        assertEquals("The quick !brown", SearchInputParsingUtils.parse("The quick! !brown", false));
        assertEquals("The quick !brown", SearchInputParsingUtils.parse("The quick! !brown", true));
        assertEquals("The quick !brown", SearchInputParsingUtils.parse("The quick! !brown*", false));
        assertEquals("The quick !brown*", SearchInputParsingUtils.parse("The quick! !brown*", true));

        assertEquals("The quick", SearchInputParsingUtils.parse("The quick-", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick-*", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick--", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick---", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick---", false));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick---*", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick-*--", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick---*", false));

        assertEquals("The quick", SearchInputParsingUtils.parse("The quick---?", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick-?--", true));
        assertEquals("The qu-ick", SearchInputParsingUtils.parse("The qu-ick---?", false));
        assertEquals("The qu-ick", SearchInputParsingUtils.parse("The qu-ick-?--", true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quick---?", false));


        assertEquals("The quick -brown", SearchInputParsingUtils.parse("The quick- -brown", false));
        assertEquals("The quick -brown", SearchInputParsingUtils.parse("The quick- -brown", true));
        assertEquals("The quick -brown", SearchInputParsingUtils.parse("The quick- -brown", false));
        assertEquals("The quick -brown", SearchInputParsingUtils.parse("The quick- -brown", true));
        assertEquals("The quick -brown", SearchInputParsingUtils.parse("The quick- -brown*", false));
        assertEquals("The quick -brown*", SearchInputParsingUtils.parse("The quick- -brown*", true));
        assertEquals("The quick -bro-wn", SearchInputParsingUtils.parse("The quick- -bro-wn*", false));
        assertEquals("The quick -bro-wn*", SearchInputParsingUtils.parse("The quick- -bro-wn*", true));

        assertEquals("", SearchInputParsingUtils.parse("!", true));
        assertEquals("", SearchInputParsingUtils.parse("!", false));
        assertEquals("", SearchInputParsingUtils.parse("!!", true));
        assertEquals("", SearchInputParsingUtils.parse("!!!", true));
        assertEquals("", SearchInputParsingUtils.parse("!!!*", true));
        assertEquals("", SearchInputParsingUtils.parse("!!!*", false));
        assertEquals("", SearchInputParsingUtils.parse("!!*!", true));
        assertEquals("", SearchInputParsingUtils.parse("!!*!", false));
        assertEquals("", SearchInputParsingUtils.parse("-", true));
        assertEquals("", SearchInputParsingUtils.parse("-", false));
        assertEquals("", SearchInputParsingUtils.parse("--", true));
        assertEquals("", SearchInputParsingUtils.parse("---", true));
        assertEquals("", SearchInputParsingUtils.parse("---*", true));
        assertEquals("", SearchInputParsingUtils.parse("---*", false));
        assertEquals("", SearchInputParsingUtils.parse("--*-", true));
        assertEquals("", SearchInputParsingUtils.parse("--*-", false));

    }

    @Test
    public void testSearchInputParsingUtils_parse_retainWordBoundaries() throws Exception {
        assertEquals("I me love Ben Jerrie''s?", SearchInputParsingUtils.parse("I(me) love Ben&Jerrie's?", true, true));
        assertEquals("I me love Ben Jerrie''s", SearchInputParsingUtils.parse("I(me) love Ben&Jerrie's?", false, true));
        assertEquals("Ime love BenJerrie''s?", SearchInputParsingUtils.parse("I[me] love Ben&Jerrie's?", true, false));
        assertEquals("Ime love BenJerrie''s", SearchInputParsingUtils.parse("I[me] love Ben&Jerrie's?", false, false));
        assertEquals("The quick \\\"brown\\\" fox can''t jump 32.3 feet, right", SearchInputParsingUtils.parse("The quick (“brown”) fox can’t jump 32.3 feet, right?\n", false, true));
        assertEquals("The quick \\\"brown\\\" fox can''t jump 32.3 feet, right?", SearchInputParsingUtils.parse("The quick (“brown”) fox can’t jump 32.3 feet, right?\n", true, true));
        assertEquals("The quick !brown*", SearchInputParsingUtils.parse("The quick! !brown*", true, true));
        assertEquals("The quick !brown", SearchInputParsingUtils.parse("The quick! !brown*", false, true));

        assertEquals("The quick br''own", SearchInputParsingUtils.parse("The quick br*'own", false, true));
        assertEquals("The quick br*''own", SearchInputParsingUtils.parse("The quick br*'own", true, true));
    }

    @Test
    public void testSearchInputParsingUtils_removeLeadingWildCardsFromWords() throws Exception {
        assertEquals("The quick brown fox jumps over the lazy dog", SearchInputParsingUtils.removeLeadingWildCardsFromWords("The quick brown fox jumps over the lazy dog"));
        assertEquals("The qui*ck brown fox jumps over the lazy dog", SearchInputParsingUtils.removeLeadingWildCardsFromWords("The qui*ck brown fox jumps over the lazy dog"));
        assertEquals("The qui*ck brown* fox jumps* over the lazy dog", SearchInputParsingUtils.removeLeadingWildCardsFromWords("The qui*ck brown* fox jumps* over the lazy dog"));
        assertEquals("The qui*ck brown* fox jumps* over the lazy dog", SearchInputParsingUtils.removeLeadingWildCardsFromWords("The *qui*ck *brown* fox jumps* over the lazy dog"));
    }

    @Test
    public void testSearchInputParsingUtils_parse_excludeAmpersand() throws Exception {
        assertEquals("The quick brown fox jumps over the lazy dog", SearchInputParsingUtils.parse("The &quick brown& fox jumps o&ver &&the l&&azy dog&&", true, false));
        assertEquals("The quick brown fox jumps o ver the l azy dog", SearchInputParsingUtils.parse("The &quick brown& fox jumps o&ver &&the l&&azy dog&&", true, true));
        assertEquals("The &quick brown& fox jumps o&ver &&the l&&azy dog&&", SearchInputParsingUtils.parse("The &quick brown& fox jumps o&ver &&the l&&azy dog&&", true, new char[]{'&'}, false));
        assertEquals("The &quick brown& fox jumps o&ver &&the l&&azy dog&&", SearchInputParsingUtils.parse("The &quick brown& fox jumps o&ver &&the l&&azy dog&&", true, new char[]{'&'}, true));
    }

    @Test
    public void testSearchInputParsingUtils_removeInvalidAndEscapeChars_removesTrailingExclamation() {
        assertEquals("No exclamation",SearchInputParsingUtils.removeInvalidAndEscapeChars("No exclamation!", false));
        assertEquals("No exclamation",SearchInputParsingUtils.removeInvalidAndEscapeChars("No exclamation!", true));
        assertEquals("No exclamation",SearchInputParsingUtils.removeInvalidAndEscapeChars("No exclamation !", false));
        assertEquals("No exclamation",SearchInputParsingUtils.removeInvalidAndEscapeChars("No exclamation !", true));
    }

    @Test
    public void testSearchInputParsingUtils_removeInvalidAndEscapeChars_removesTrailingDash() {
        assertEquals("No dash",SearchInputParsingUtils.removeInvalidAndEscapeChars("No dash-", false));
        assertEquals("No dash",SearchInputParsingUtils.removeInvalidAndEscapeChars("No dash-", true));
        assertEquals("No dash",SearchInputParsingUtils.removeInvalidAndEscapeChars("No dash -", false));
        assertEquals("No dash",SearchInputParsingUtils.removeInvalidAndEscapeChars("No dash -", true));
    }

    @Test
    public void testSearchInputParsingUtils_parse_differentApostrophes() throws Exception {
        assertEquals("The quic''k", SearchInputParsingUtils.parse("The quic'k", true));
        assertEquals("The quic''k", SearchInputParsingUtils.parse("The quic’k", true));
        assertEquals("The quic''k", SearchInputParsingUtils.parse("The quic‘k", true));
        assertEquals("The quic''k", SearchInputParsingUtils.parse("The quic'k", true, false));
        assertEquals("The quic''k", SearchInputParsingUtils.parse("The quic’k", true, false));
        assertEquals("The quic''k", SearchInputParsingUtils.parse("The quic‘k", true, false));
    }

    @Test
    public void testSearchInputParsingUtils_remove_backslash() throws Exception {
        assertEquals("The quic k", SearchInputParsingUtils.parse("The quic\\k", true, true));
        assertEquals("The quick", SearchInputParsingUtils.parse("The quic\\k", true, false));
        assertEquals("", SearchInputParsingUtils.parse("\\", true, true));
        assertEquals("", SearchInputParsingUtils.parse("\\", true, false));
    }

    @Test
    public void testSearchInputParsingUtils_escape_doublequote() throws Exception {
        assertEquals("The q\\\"uick", SearchInputParsingUtils.parse("The q\"uick", false, true));
        assertEquals("The q\\\"uick", SearchInputParsingUtils.parse("The q\"uick", false, false));
        assertEquals("\\\"", SearchInputParsingUtils.parse("\"", false, true));
        assertEquals("\\\"", SearchInputParsingUtils.parse("\"", false, false));
    }

}

/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.serialize;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CharacterReferenceNormalizerTest {

    @Test
    public void testCharacterEntityConversion() throws Exception {
        final String result = CharacterReferenceNormalizer.normalize("&nbsp; &gt; &lt; &amp; á &aacute;");
        assertEquals("&nbsp; &gt; &lt; &amp; á á", result);
    }

    @Test
    public void testQuoteConversion() throws Exception {
        final String result = CharacterReferenceNormalizer.normalize("' \" &apos; &quot;");
        assertEquals("' \" ' \"", result);
    }

    @Test
    public void test_base_entities_are_encoded_as_entities() {
        assertEquals("&gt; &lt; &amp; &nbsp;", CharacterReferenceNormalizer.normalize("> < & " + (char) 160));
    }

    @Test
    public void test_base_entities_remain_encoded_as_entities() {
        assertEquals("&gt; &lt; &amp; &nbsp;", CharacterReferenceNormalizer.normalize("&gt; &lt; &amp; &nbsp;"));
    }

    @Test
    public void test_numeric_entities_are_converted_to_characters() {
        assertEquals("á á á á", CharacterReferenceNormalizer.normalize("&#225; &#xe1; &#X00E1; &#0x0e1;"));
        assertEquals("&gt; &lt; &amp; &nbsp;", CharacterReferenceNormalizer.normalize("&#62; &#60; &#38; &#160;"));
    }

    @Test
    public void test_named_entities_are_converted_to_characters() {
        assertEquals("á", CharacterReferenceNormalizer.normalize("&aacute;"));
    }

    @Test
    public void test_named_entities_with_numbers_are_converted() {
        assertEquals("¾", CharacterReferenceNormalizer.normalize("&frac34;"));
    }

    @Test
    public void test_incorrect_numeric_entity_conversion() {
        assertEquals("&amp;#12ab; &amp;#x12abz;", CharacterReferenceNormalizer.normalize("&#12ab; &#x12abz;"));
        assertEquals("&amp;#", CharacterReferenceNormalizer.normalize("&#"));
        assertEquals("&amp;#12", CharacterReferenceNormalizer.normalize("&#12"));
    }

    @Test
    public void test_incorrect_named_entity_conversion() {
        assertEquals("&amp;nonsense;", CharacterReferenceNormalizer.normalize("&nonsense;"));
        assertEquals("&amp;", CharacterReferenceNormalizer.normalize("&"));
        assertEquals("&amp;nonsense", CharacterReferenceNormalizer.normalize("&nonsense"));
    }

    @Test
    public void test_incorrect_entity_directly_followed_by_correct_entity_conversion() {
        assertEquals("&amp;nonsenseá", CharacterReferenceNormalizer.normalize("&nonsense&#225;"));
        assertEquals("&amp;#225á", CharacterReferenceNormalizer.normalize("&#225&#225;"));
    }
}

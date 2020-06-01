/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.SpecialEntities;
import org.htmlcleaner.SpecialEntity;

public class CharacterReferenceNormalizer {

    private enum ConvertQuote { YES, NO };

    private static class ResultWriter {
        private final StringBuilder result;
        private final ConvertQuote convertQuote;

        private ResultWriter(final int capacity, final ConvertQuote convertQuote) {
            result = new StringBuilder(capacity);
            this.convertQuote = convertQuote;
        }

        @Override
        public String toString() {
            return result.toString();
        }

        ResultWriter write(final char ch) {
            switch (ch) {
                case '"':
                    result.append(this.convertQuote == ConvertQuote.NO ? "&quot;" : ch);
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case (char)160:
                    result.append("&nbsp;");
                    break;
                default:
                    result.append(ch);
            }
            return this;
        }
    }

    private static class CharacterReference {
        final int position;
        final String string;
        final char character;
        CharacterReference(final int position, final String string, final char character) {
            this.position = position;
            this.string = string;
            this.character = character;
        }
    }

    private static class CharacterReferenceFinder {
        private final static Pattern pattern = Pattern.compile(
                "&(?<entity>\\p{Alnum}+);|&#(?<dec>\\p{Digit}+);|&#0*(x|X)(?<hex>\\p{XDigit}+);");
        /* Regexp with searching for 3 patterns:
         * 1) character entity reference, e.g. &aacute;
         * 2) decimal numeric character reference, e.g. &#225;
         * 3) hexadecimal numeric character reference, e.g. &#0xE1;
         */

        private static final SpecialEntities specialEntities = SpecialEntities.INSTANCE;

        private final Matcher matcher;

        CharacterReferenceFinder(final String string) {
            matcher = pattern.matcher(string);
        }

        /**
         * Finds the next valid {@link CharacterReference} in the text. It uses {@link #specialEntities} to determine
         * whether a character entity reference is valid. Invalid references such as &amp;nonsense; are skipped.
         * @return the next valid {@link CharacterReference} or null if none was found
         */
        CharacterReference findNextValid() {
            while (matcher.find()) {
                final String reference = matcher.group(0);

                final String entityGroup = matcher.group("entity");
                if (entityGroup != null) {
                    final SpecialEntity specialEntity = specialEntities.getSpecialEntity(entityGroup);
                    if (specialEntity != null) {
                        return new CharacterReference(matcher.start(), reference, specialEntity.charValue());
                    } else {
                        continue;
                    }
                }

                final String decGroup = matcher.group("dec");
                if (decGroup != null) {
                    return new CharacterReference(matcher.start(), reference, (char)Integer.parseInt(decGroup));
                }

                final String hexGroup = matcher.group("hex");
                if (hexGroup != null) {
                    return new CharacterReference(matcher.start(), reference, (char)Integer.parseInt(hexGroup, 16));
                }
            }

            return null;
        }

    }

    /**
     * @deprecated use {@link #normalizeElementContent(String)} instead.
     */
    @Deprecated
    public static String normalize(final String string) {
        return normalizeElementContent(string);
    }

    public static String normalizeElementContent(final String string) {
        return normalize(string, ConvertQuote.YES);
    }

    public static String normalizeAttributeContent(final String string) {
        return normalize(string, ConvertQuote.NO);
    }

    /**
     * Transforms character references (e.g. &amp;aacute;, &amp;#225;, etc.) to characters by applying the same rules as
     * CKEditor in Hippo's default configuration. These rules are: convert all character references to the character
     * they represent except for &amp;nbsp;, &amp;gt;, &amp;lt;, &amp;amp; - those must always be encoded.
     *
     * @param string the string to normalize
     * @param convertQuote whether to convert &quot; to " or not.
     * @return the normalized string
     */
    private static String normalize(final String string, final ConvertQuote convertQuote) {
        final ResultWriter resultWriter = new ResultWriter(string.length(), convertQuote);
        final CharacterReferenceFinder finder = new CharacterReferenceFinder(string);
        int current = 0;

        CharacterReference reference;
        while ((reference = finder.findNextValid()) != null) {
            while (current < reference.position) {
                resultWriter.write(string.charAt(current));
                current++;
            }
            resultWriter.write(reference.character);
            current += reference.string.length();
        }

        while (current < string.length()) {
            resultWriter.write(string.charAt(current));
            current++;
        }

        return resultWriter.toString();
    }

}

package org.hippoecm.hst.util;

/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.site.HstServices;

/**
 * Input utilities for user searches.
 *
 */
public final class SearchInputParsingUtils {

    private static final String FQCN = SearchInputParsingUtils.class.getName();
    
    private static final String WHITESPACE_PATTERN = "\\s+";

    private SearchInputParsingUtils() {

    }

    /**
     * Returns a parsed version of the input
     * @param input the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still not allowed as leading for a term)
     * @return the parsed version of the <code>input</code>. When <code>input</code> is <code>null</code>, <code>null</code> is returned
     */
    public static String parse(final String input,final boolean allowSingleNonLeadingWildCardPerTerm) {
        if(input == null) {
            return null;
        }        
        String parsed = compressWhitespace(input);
        parsed = removeInvalidAndEscapeChars(parsed, allowSingleNonLeadingWildCardPerTerm);
        parsed = removeLeadingOrTrailingOrOperator(parsed);
        parsed = rewriteNotOperatorsToMinus(parsed);
        parsed = removeLeadingAndTrailingAndReplaceWithSpaceAndOperators(parsed);
        parsed = EncodingUtils.isoLatin1AccentReplacer(parsed);
        HstServices.getLogger(FQCN, FQCN).debug("Rewrote input '{}' to '{}'", input, parsed);
        return parsed;
    }
    
    /**
     * Returns a parsed version of the input
     * @param input the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still not allowed as leading for a term)
     * @param maxLength the maxLength of the returned parsed input
     * @return the parsed version of the <code>input</code>. When <code>input</code> is <code>null</code>, <code>null</code> is returned
     */
    public static String parse(final String input,final boolean allowSingleNonLeadingWildCardPerTerm, int maxLength) {
        if(input == null) {
            return null;
        }
        String parsed = parse(input, allowSingleNonLeadingWildCardPerTerm);
        if(parsed.length() > maxLength) {
            parsed = parsed.substring(0, maxLength);
        }
        HstServices.getLogger(FQCN, FQCN).debug("Rewrote input '{}' to '{}'", input, parsed);
        return parsed;
    }

    public static String removeLeadingWildCardsFromWords(final String input) {
        if(input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
          char c = input.charAt(i);
          // Some of these characters break the jcr query and others like * and ? have a very negative impact 
          // on performance. 
            if (c == '*' || c == '?') {
                if (sb.length() > 0) {
                    char prevChar = sb.charAt(sb.length() - 1);
                    if (!(prevChar == '\"' || prevChar == '\'' || prevChar == ' ')) {
                        sb.append(c);
                    }
                }
            } else {
                sb.append(c);
            }
        }
        String output = sb.toString();
        if(!input.equals(output)) {
            HstServices.getLogger(FQCN, FQCN).debug("Rewrote input '{}' to '{}'", input, output);
        }
        return output;
    }
    
    /**
     * <p>
     * Removes invalid chars, escapes some chars. If <code>allowSingleNonLeadingWildCard</code> is <code>true</code>, there 
     * is one single non leading <code>*</code> or <code>?</code> allowed. Note, that this wildcard is not allowed to be 
     * leading of a new word.
     * </p>
     * <p>
     * Recommended is to remove all wildcards
     * </p> 
     * @param input
     * @param allowSingleNonLeadingWildCardPerTerm
     * @return formatted version of <code>input</code>
     */
    public static String removeInvalidAndEscapeChars(final String input, final boolean allowSingleNonLeadingWildCardPerTerm) {
        if(input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        StringBuffer sb = new StringBuffer();
        boolean allowWildCardInCurrentTerm = allowSingleNonLeadingWildCardPerTerm;
        for (int i = 0; i < input.length(); i++) {
          char c = input.charAt(i);
          // Some of these characters break the jcr query and others like * and ? have a very negative impact 
          // on performance. 
          if ( c == '(' || c == ')' || c == '^' || c == '[' || c == ']' || c == '{' 
              || c == '}' || c == '~' || c == '*' || c == '?' || c == '|' || c == '&') {
              // ~ is used for synonyms search: This is jackrabbit specific and different than lucene fuzzy
              // see http://wiki.apache.org/jackrabbit/SynonymSearch. It is only allowed to be the first char 
              // of a word
              if(c == '~') {
                  if(sb.length() == 0) {
                      sb.append(c);
                  } else {
                      char prevChar = sb.charAt(sb.length() - 1);
                      if(prevChar == ' ') {
                          sb.append(c);
                      }
                      // else we remove the ~ 
                  }
              } else if(sb.length() > 0) {
                  // if one wildcard is allowed, it will be added but never as leading
                  if(c == '*' || c == '?') {
                      if(allowWildCardInCurrentTerm) {
                          // check if prev char is not a space or " or  '
                          // i must be > 0 here
                          char prevChar = sb.charAt(sb.length() -1);
                          if(!(prevChar == '\"' || prevChar == '\'' || prevChar == ' ')) {
                              sb.append(c);
                              allowWildCardInCurrentTerm = false;
                          }
                      } 
                  }
              }       
          } else if (c == '\"') {
              sb.append('\\');
              sb.append(c);
          } else if (c == '\'') {
              // we strip ' because jackrabbit xpath builder breaks on \' (however it should be possible according spec)
          } else if (c == ' ') {
              // next term. set allowWildCardInCurrentTerm again to allowSingleNonLeadingWildCardPerTerm
              allowWildCardInCurrentTerm = allowSingleNonLeadingWildCardPerTerm;
              sb.append(c);
          } else {
              sb.append(c);
          }
        }
        String output = sb.toString();
        if(!input.equals(output)) {
            HstServices.getLogger(FQCN, FQCN).debug("Rewrote input '{}' to '{}'", input, output);
        }
        return output;
      }
    
    /**
     * Rewrites any "NOT" operators in the keywords to a minus symbol (-). This is necessary because Jackrabbit doesn't
     * fully support the Lucene query language. Jackrabbit <em>does</em> support the minus symbol to exclude keywords
     * from search results but <em>does not</em> support the "NOT" keyword.
     *
     * @param input keywords to rewrite
     * @return rewritten input
     */
    private static String rewriteNotOperatorsToMinus(String input) {
        if(input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        return input.replace("NOT ", "-");
    }

    /**
     * Removes any "AND" operators in the keywords. This is necessary because Jackrabbit doesn't
     * fully support the Lucene query language. Lucene by default applies "and" to all keywords so to support the "AND"
     * operator it can be simply removed from the keywords.
     *
     * @param input keywords to rewrite
     * @return rewritten input
     */
    private static String removeLeadingAndTrailingAndReplaceWithSpaceAndOperators(String input) {
        if(input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        String output = input;
        output = StringUtils.removeStart(output, "AND ");
        output = StringUtils.removeEnd(output, " AND");
        return output.replace(" AND ", " ");
    }
    
    /**
     * Removes the logical operator "OR" at the end of the query. Otherwise this will result in a Lucene parse exception.
     *
     * @param input the original (possibly invalid) query string
     * @return a valid query string
     */
    public static String removeLeadingOrTrailingOrOperator(String input) {
        if(input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        String output = input;
        output = StringUtils.removeStart(output, "OR ");
        output = StringUtils.removeEnd(output, " OR");
        return output;
    }
    
    /**
     * Compress whitespace (tab, newline, multiple spaces) by removing leading and trailing whitespace, and reducing
     * inbetween whitespace to one space.
     *
     * @param text the text to compress (may be null)
     * @return the compressed text, or null if the text to compress was null
     */
    public static String compressWhitespace(String text) {
        if (text == null) {
            return null;
        }
        String trimmedText = StringUtils.trim(text);
        return trimmedText.replaceAll(WHITESPACE_PATTERN, " ");
    }


}

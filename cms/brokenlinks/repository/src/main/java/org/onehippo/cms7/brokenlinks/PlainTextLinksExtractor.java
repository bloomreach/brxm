/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.brokenlinks;

import java.util.ArrayList;
import java.util.List;

/**
 * plain text links extractor uses String parsing to find href and src attributes in a String The String does not need
 * to be valid xml or to be parsable as Dom
 */
public class PlainTextLinksExtractor {

    protected static final String LINK_TAG = "<a";
    protected static final String IMG_TAG = "<img";
    protected static final String END_TAG = ">";
    protected static final String HREF_ATTR_NAME = "href=";
    protected static final int HREF_ATTR_NAME_LENGTH = 5;
    protected static final String SRC_ATTR_NAME = "src=";
    protected static final int SRC_ATTR_NAME_LENGTH = 4;
    protected static final String DOUBLE_QUOTE = "\"";
    protected static final String SINGLE_QUOTE = "\'";

    public static final String SPACE = " ";

    /**
     * Parses text. Duplicate urls are collapsed into one single url. A url is considered a url for <ul> <li>An href
     * attr in a link &lt;a&gt; element that contains SCHEME_SUFFIX and starts with one of the PROTOCOLS</li> <li>A src
     * attr in a image lt;img&gt;element that contains SCHEME_SUFFIX and starts with one of the PROTOCOLS</li> </ul>
     *
     * @param text the <code>text</code> to parse urls from. <code>text</code> is not allowed to be <code>null</code>
     * @return the List of unique urls and empty list if no urls were found
     * @throws IllegalStateException when text for some reason cannot be parsed for links. This indicates a programming
     *                               flaw if it happens
     */
    public static List<String> getLinks(final String text) throws IllegalStateException {
        List<String> urls = new ArrayList<String>();
        parse(text, urls);
        return urls;
    }

    /**
     * parses the <code>text</code> and adds all links to <code>urls</code>
     *
     * @throws IllegalStateException when text for some reason cannot be parsed: This indicates a programming flaw if it
     *                               happens
     */
    public static void parse(final String text, final List<String> urls) throws IllegalStateException {

        // we use lowercased text version for checking where the link and image tag are. To get the actual value for the
        // src or href attribute, we need the original non-lowercased value
        final String lowercasedText = text.toLowerCase();
        int totalSize = text.length();
        {
            int globalOffset = 0;
            int nextLinkOffset;
            // First check all <a> link elements
            while ((nextLinkOffset = lowercasedText.indexOf(LINK_TAG, globalOffset)) != -1) {
                globalOffset = nextLinkOffset + LINK_TAG.length();

                // fetch href after nextLinkOffset
                int hrefAttrValueStart = lowercasedText.indexOf(HREF_ATTR_NAME, nextLinkOffset);
                if (hrefAttrValueStart == -1) {
                    // we're done, no more href
                    break;
                }
                hrefAttrValueStart = hrefAttrValueStart + HREF_ATTR_NAME_LENGTH;
                if (hrefAttrValueStart == totalSize) {
                    // broken html, we have reached the end
                    break;
                }
                // check whether next char is a ' or "
                boolean hrefWithinQuotes = false;
                char next = lowercasedText.charAt(hrefAttrValueStart);
                if (next == '\'' || next == '\"') {
                    hrefWithinQuotes = true;
                    hrefAttrValueStart++;
                }

                int hrefAttrValueEnd;

                if (hrefWithinQuotes) {
                    int nextSingleQuote = lowercasedText.indexOf(DOUBLE_QUOTE, hrefAttrValueStart);
                    int nextDoubleQoute = lowercasedText.indexOf(SINGLE_QUOTE, hrefAttrValueStart);
                    hrefAttrValueEnd = getMinimalPositiveInt(nextSingleQuote, nextDoubleQoute);
                } else {
                    // until the next space OR until next >  : Pick the smallest
                    int nextSpace = lowercasedText.indexOf(SPACE, hrefAttrValueStart);
                    int nextCloseTag = lowercasedText.indexOf(END_TAG, hrefAttrValueStart);
                    hrefAttrValueEnd = getMinimalPositiveInt(nextSpace, nextCloseTag);
                }
                int endTagOffset = lowercasedText.indexOf(END_TAG, nextLinkOffset);

                if (endTagOffset > -1) {
                    globalOffset = endTagOffset;
                }
                // Sanity check : The hrefStart and hrefEnd both must be smaller than endTagOffset
                // if not, it was a link element that for example was empty, like <a/>

                if (hrefAttrValueStart < endTagOffset && hrefAttrValueEnd <= endTagOffset) {
                    // below create a new String from the substring, otherwise, url will
                    // keep a reference to the *entire* 'text' String as this is how substring works
                    // use the orignal 'text' and not lowercasedText to extract the href
                    // thus on purpose I use new String(text.substring(hrefStart, hrefEnd))
                    String url = new String(text.substring(hrefAttrValueStart, hrefAttrValueEnd));

                    if (!urls.contains(url)) {
                        urls.add(url);
                    }
                }
            }
        }

        {
            int globalOffset = 0;
            int nextImgOffset;
            // First check all <img> link elements
            while ((nextImgOffset = lowercasedText.indexOf(IMG_TAG, globalOffset)) != -1) {
                globalOffset = nextImgOffset + IMG_TAG.length();

                // fetch href after nextLinkOffset
                int srcAttrValueStart = lowercasedText.indexOf(SRC_ATTR_NAME, nextImgOffset);
                if (srcAttrValueStart == -1) {
                    // we're done, no more href
                    break;
                }
                srcAttrValueStart = srcAttrValueStart + SRC_ATTR_NAME_LENGTH;
                if (srcAttrValueStart == totalSize) {
                    // broken html, we have reached the end
                    break;
                }
                // check whether next char is a ' or "
                boolean srcWithinQuotes = false;
                char next = lowercasedText.charAt(srcAttrValueStart);
                if (next == '\'' || next == '\"') {
                    srcWithinQuotes = true;
                    srcAttrValueStart++;
                }

                int srcAttrValueEnd;

                if (srcWithinQuotes) {
                    int nextSingleQuote = lowercasedText.indexOf(DOUBLE_QUOTE, srcAttrValueStart);
                    int nextDoubleQoute = lowercasedText.indexOf(SINGLE_QUOTE, srcAttrValueStart);
                    srcAttrValueEnd = getMinimalPositiveInt(nextSingleQuote, nextDoubleQoute);
                } else {
                    // until the next space OR until next >  : Pick the smallest
                    int nextSpace = lowercasedText.indexOf(SPACE, srcAttrValueStart);
                    int nextCloseTag = lowercasedText.indexOf(END_TAG, srcAttrValueStart);
                    srcAttrValueEnd = getMinimalPositiveInt(nextSpace, nextCloseTag);
                }
                int endTagOffset = lowercasedText.indexOf(END_TAG, nextImgOffset);

                if (endTagOffset > -1) {
                    globalOffset = endTagOffset;
                }
                // Sanity check : The hrefStart and hrefEnd both must be smaller than endTagOffset
                // if not, it was a link element that for example was empty, like <a/>

                if (srcAttrValueStart < endTagOffset && srcAttrValueEnd <= endTagOffset) {
                    // below create a new String from the substring, otherwise, url will
                    // keep a reference to the *entire* 'text' String as this is how substring works
                    // use the orignal 'text' and not lowercasedText to extract the href
                    // thus on purpose I use new String(text.substring(hrefStart, hrefEnd))
                    String url = new String(text.substring(srcAttrValueStart, srcAttrValueEnd));

                    if (!urls.contains(url)) {
                        urls.add(url);
                    }
                }

            }
        }
    }

    /**
     * @return returns the minimal of <code>value1/code> and <code>value2</code> and 0 if they are both negative
     */
    private static int getMinimalPositiveInt(final int value1, final int value2) {
        if (value1 > -1 && (value1 <= value2 || value2 < 0)) {
            return value1;
        }
        if (value2 > -1) {
            return value2;
        }
        return 0;
    }
}

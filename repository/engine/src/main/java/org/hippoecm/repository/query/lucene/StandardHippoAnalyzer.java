/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.query.lucene;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The standard multi language ECM analyzer, taking care of removing diacritics and stopwords for most common languages.
 * <P>
 * This analyzer implementation reads stopwords from its resource bundle files. e.g, StandardHippoAnalyzer_en.properties.
 * All the available locale strings are determined by the <code>stopwords.locales</code> property in StandardHippoAnalyzer.properties.
 * By the available locale strings (split by comma and trimmed), this loads all the stopwords from each localized
 * ResourceBundle files (e.g, StandardHippoAnalyzer_en.properties, StandardHippoAnalyzer_es.properties, StandardHippoAnalyzer_pt_BR.properties, etc.).
 * </P>
 * <P>
 * Therefore, it is possible to customize the stopwords definitions for each locale, and add or remove a localized
 * stopwords definition file, too, by updating the ResourceBundle files.
 * </P>
 */
public final class StandardHippoAnalyzer extends Analyzer {

    private static Logger log = LoggerFactory.getLogger(StandardHippoAnalyzer.class);

    /**
     * @deprecated since 5.0 (cms 12.0). Locale specific stop words will not be provided in public API in the future.
     */
    @Deprecated
    public static final String[] CJK_STOP_WORDS;

    /**
     * @deprecated since 5.0 (cms 12.0). Locale specific stop words will not be provided in public API in the future.
     */
    @Deprecated
    public static final String[] SPANISH_STOP_WORDS;

    /**
     * @deprecated since 5.0 (cms 12.0). Locale specific stop words will not be provided in public API in the future.
     */
    @Deprecated
    public static final String[] DUTCH_STOP_SET;

    /**
     * @deprecated since 5.0 (cms 12.0). Locale specific stop words will not be provided in public API in the future.
     */
    @Deprecated
    public static final String[] GERMAN_STOP_WORDS;

    /**
     * @deprecated since 5.0 (cms 12.0). Locale specific stop words will not be provided in public API in the future.
     */
    @Deprecated
    public static final String[] FRENCH_STOP_WORDS;

    /**
     * @deprecated since 5.0 (cms 12.0). Locale specific stop words will not be provided in public API in the future.
     */
    @Deprecated
    public static final String[] BRAZILIAN_STOP_WORDS;

    /**
     * @deprecated since 5.0 (cms 12.0). Locale specific stop words will not be provided in public API in the future.
     */
    @Deprecated
    public static final String[] CZECH_STOP_WORDS;

    /**
     * @deprecated since 5.0 (cms 12.0). Locale specific stop words will not be provided in public API in the future.
     */
    @Deprecated
    public static final String[] ALL_STOP_WORDS;

    /**
     * @deprecated since 5.0 (cms 12.0). Locale specific stop words will not be provided in public API in the future.
     */
    @Deprecated
    public static final Set<String> DEFAULT_STOP_SET;

    private static final String FQCN = StandardHippoAnalyzer.class.getName();

    private static final Map<Locale, String[]> localeStopWordsMap = new HashMap<>();

    private static final String STOPWORDS_LOCALES = "stopwords.locales";

    private static final String STOPWORDS_SPLIT_DELIMITERS = "stopwords.split.delimiters";

    private static final String STOPWORDS_SPLIT_PRESERVE_ALL_TOKENS = "stopwords.split.preserveAllTokens";

    private static final String STOPWORDS_SPLIT_TOKENS = "stopwords.split.tokens";

    static {
        final String[] availableLocaleStrings = getAvailableLocaleStringsFromStopWordsProperties();
        Locale locale;
        final List<String> allStopWordsList = new LinkedList<>();
        String[] localeStopWords;

        for (String localeString : availableLocaleStrings) {
            locale = LocaleUtils.toLocale(localeString);
            localeStopWords = getLocaleSpecificStopWordsArrayFromResourceBundle(locale);
            localeStopWordsMap.put(locale, localeStopWords);
            allStopWordsList.addAll(Arrays.asList(localeStopWords));
        }

        ALL_STOP_WORDS = allStopWordsList.toArray(new String[allStopWordsList.size()]);

        DEFAULT_STOP_SET = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(ALL_STOP_WORDS)));

        CJK_STOP_WORDS = defaultIfNull(localeStopWordsMap.get(LocaleUtils.toLocale("en")));

        SPANISH_STOP_WORDS = defaultIfNull(localeStopWordsMap.get(LocaleUtils.toLocale("es")));

        DUTCH_STOP_SET = defaultIfNull(localeStopWordsMap.get(LocaleUtils.toLocale("nl")));

        GERMAN_STOP_WORDS = defaultIfNull(localeStopWordsMap.get(LocaleUtils.toLocale("de")));

        FRENCH_STOP_WORDS = defaultIfNull(localeStopWordsMap.get(LocaleUtils.toLocale("fr")));

        BRAZILIAN_STOP_WORDS = defaultIfNull(localeStopWordsMap.get(LocaleUtils.toLocale("pt_BR")));

        CZECH_STOP_WORDS = defaultIfNull(localeStopWordsMap.get(LocaleUtils.toLocale("cs")));
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        TokenStream result = new ClassicTokenizer(Version.LUCENE_36, reader);
        result = new StandardFilter(Version.LUCENE_36, result);
        result = new LowerCaseFilter(Version.LUCENE_36, result);
        result = new StopFilter(Version.LUCENE_36, result, DEFAULT_STOP_SET);
        result = new ASCIIFoldingFilter(result);
        return result;
    }

    static String [] getAvailableLocaleStringsFromStopWordsProperties() {
        String [] localeStrings = null;

        InputStream is = null;
        BufferedInputStream bis = null;
        final String propResourceName = StandardHippoAnalyzer.class.getSimpleName() + ".properties";

        try {
            is = StandardHippoAnalyzer.class.getResourceAsStream(propResourceName);
            bis = new BufferedInputStream(is);
            Properties props = new Properties();
            props.load(bis);
            localeStrings = StringUtils.split(props.getProperty(STOPWORDS_LOCALES), ", ");
        } catch (IOException e) {
            log.error("Failed to load stopwords properties.", e);
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(is);
        }

        if (localeStrings == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        return localeStrings;
    }

    private static String [] getLocaleSpecificStopWordsArrayFromResourceBundle(final Locale locale) {
        String [] stopWordsArray = null;

        ResourceBundle bundle = null;

        try {
            bundle = ResourceBundle.getBundle(FQCN, locale);
        } catch (MissingResourceException e) {
            log.error("Cannot find stopwords resource bundle for locale: {}", locale, e);
        }

        String delimiters = ",";
        boolean preserveAllTokens = false;

        if (bundle != null) {
            try {
                delimiters = bundle.getString(STOPWORDS_SPLIT_DELIMITERS);
            } catch (MissingResourceException e) {
                log.info("Key, {}, not found for locale: {}. Comma will be used by default.", STOPWORDS_SPLIT_DELIMITERS, locale);
            }

            try {
                preserveAllTokens = BooleanUtils.toBoolean(bundle.getString(STOPWORDS_SPLIT_PRESERVE_ALL_TOKENS));
            } catch (MissingResourceException e) {
                log.info("Key, {}, not found for locale: {}. Set to false by default.", STOPWORDS_SPLIT_PRESERVE_ALL_TOKENS, locale);
            }

            try {
                if (preserveAllTokens) {
                    stopWordsArray = StringUtils.splitPreserveAllTokens(bundle.getString(STOPWORDS_SPLIT_TOKENS), delimiters);
                } else {
                    stopWordsArray = StringUtils.split(bundle.getString(STOPWORDS_SPLIT_TOKENS), delimiters);
                }
            } catch (MissingResourceException e) {
                log.error("Key, {}, not found for locale: {}.", STOPWORDS_SPLIT_TOKENS, locale, e);
            }
        }

        if (stopWordsArray == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        return stopWordsArray;
    }

    private static String [] defaultIfNull(String [] array) {
        if (array == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return array;
    }
}

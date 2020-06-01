/**
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.resourcebundle;

import static org.onehippo.repository.util.JcrConstants.JCR_DATA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceBundleUtils
 * <P>
 * Utility to get resource bundle from either HST ResourceBundleRegistry or Java Standard Resource Bundles or from
 * Repository stored resource bundle files. In case of repository stored resource bundles, the bundle nodes must be
 * of type nt:file containing jcr:content that contain Java Standard Resource Bundle files.
 * </P>
 */
public class ResourceBundleUtils {

    private static final Logger log = LoggerFactory.getLogger(ResourceBundleUtils.class);
    public static final String PROPERTIES_SUFFIX = ".properties";
    public static final int PROPERTIES_SUFFIX_LENGTH = ".properties".length();

    private ResourceBundleUtils() {
    }

    /**
     * Returns resource bundle based on the specified basename and locale.
     * If the locale is null, then the default locale (Locale.getDefault()) is used.
     * If the specific resource bundle is not found from the HST ResourceBundleRegistry, then it looks up Java standard resource bundles.
     * It may throw java.util.MissingResourceException if a resource is not found when Java standard resource bundle resources are looked up.
     * @param basename
     * @param locale
     * @return
     * @throws java.lang.NullPointerException - if baseName is null
     * @throws java.util.MissingResourceException - if no Java standard resource bundle for the specified base name can be found
     */
    public static ResourceBundle getBundle(String basename, Locale locale) {
        return getBundle(basename, locale, true);
    }

    /**
     * Returns resource bundle based on the specified basename and locale. If the locale is null, then the default
     * locale (Locale.getDefault()) is used. If the specific resource bundle is not found from the HST
     * ResourceBundleRegistry, then it looks up Java standard resource bundles when the fallbackToJavaResourceBundle is
     * true. It may throw java.util.MissingResourceException if a resource is not found when Java standard resource
     * bundle resources are looked up.
     *
     * @param basename
     * @param locale
     * @param fallbackToJavaResourceBundle
     * @throws java.lang.NullPointerException     - if baseName is null
     * @throws java.util.MissingResourceException - if no Java standard resource bundle for the specified base name can
     *                                            be found
     */
    public static ResourceBundle getBundle(String basename, Locale locale, boolean fallbackToJavaResourceBundle) {
        ResourceBundle bundle = null;
        HstRequestContext context = RequestContextProvider.get();
        if (context != null) {
            ResourceBundleRegistry resourceBundleRegistry =
                    HstServices.getComponentManager().getComponent(ResourceBundleRegistry.class.getName());

            if (resourceBundleRegistry != null) {
                boolean preview = context.isPreview();

                if (locale == null) {
                    bundle = (preview ? resourceBundleRegistry.getBundleForPreview(basename) : resourceBundleRegistry.getBundle(basename));
                } else {
                    bundle = (preview ? resourceBundleRegistry.getBundleForPreview(basename, locale) : resourceBundleRegistry.getBundle(basename, locale));
                }
            }
        }

        if (bundle == null && fallbackToJavaResourceBundle) {
            if (locale == null) {
                bundle = ResourceBundle.getBundle(basename, Locale.getDefault(), Thread.currentThread().getContextClassLoader());
            } else {
                bundle = ResourceBundle.getBundle(basename, locale, Thread.currentThread().getContextClassLoader());
            }
        }
        return bundle;
    }

    /**
     * @param session the jcr session to build the resource bundles with
     * @param baseJcrAbsFilePath  the absolute jcr path to the base resource bundle, which must be a path starting with
     *                            a <code>/</code> and ending with <code>.properties</code> and most point to a jcr node
     *                            of type <code>nt:file</code>
     * @param locale the {@link java.util.Locale} to get the resource bundle for. If <code>null</code> the base resource
     *               bundle is returned
     * @return the repository based {@link ResourceBundle} for <code>baseJcrAbsFilePath</code> and <code>locale</code>
     * @exception java.lang.IllegalArgumentException if no resource bundle for the specified <code>baseJcrAbsFilePath</code>
     *            can be found or in case <code>baseJcrAbsFilePath</code> is <code>null</code>
     * @exception java.lang.IllegalStateException in case an exception occurs
     */
    public static ResourceBundle getBundle(final Session session, final String baseJcrAbsFilePath, final Locale locale) {

        try {
            if (baseJcrAbsFilePath == null || !baseJcrAbsFilePath.startsWith("/") || !baseJcrAbsFilePath.endsWith(PROPERTIES_SUFFIX)) {
                throw new IllegalArgumentException("baseJcrAbsFilePath is not allowed to be null, must " +
                        "start with a '/' and must end with .properties.");
            }

            if (!session.nodeExists(baseJcrAbsFilePath)) {
                String msg = String.format("baseJcrAbsFilePath '%s' does not point to an existing jcr Node.", baseJcrAbsFilePath);
                throw new IllegalArgumentException(msg);
            }

            final Node baseNode = session.getNode(baseJcrAbsFilePath);
            if (!baseNode.isNodeType(JcrConstants.NT_FILE)) {
                String msg = String.format("baseJcrAbsFilePath '%s' must point to a jcr Node of type '%s' but was of type '%s'.",
                        baseJcrAbsFilePath, JcrConstants.NT_FILE, baseNode.getPrimaryNodeType().getName());
                throw new IllegalArgumentException(msg);
            }

            final Node content = baseNode.getNode(JcrConstants.JCR_CONTENT);
            final Binary binary = content.getProperty(JCR_DATA).getBinary();

            final List<ResourceBundle> bundles = new ArrayList<>();
            final PropertyResourceBundle defaultBundle = new PropertyResourceBundle(binary.getStream());

            if (locale != null) {

                List<Locale> lookupLocales = localeLookupList(locale);
                for (Locale loc : lookupLocales) {
                    if (loc.toString().isEmpty()) {
                        continue;
                    }
                    try {
                        String localePath = getLocalePathForBase(baseJcrAbsFilePath, loc);
                        if (!session.nodeExists(localePath)) {
                            log.debug("Resource bundle for locale '{}' does not exist. Skip.", localePath);
                            continue;
                        }
                        final  Node localeNode = session.getNode(localePath);
                        if (!localeNode.isNodeType(JcrConstants.NT_FILE)) {
                            log.warn("localePath '{}' must point to a jcr Node of type '{}' but was of type '{}'. " +
                                    "Skip locale '{}'.", baseJcrAbsFilePath, JcrConstants.NT_FILE,
                                    localeNode.getPrimaryNodeType().getName(), loc);
                            continue;
                        }
                        final Node localeContent = localeNode.getNode(JcrConstants.JCR_CONTENT);
                        final Binary localeBinary = localeContent.getProperty(JCR_DATA).getBinary();
                        final PropertyResourceBundle localeBundle = new PropertyResourceBundle(localeBinary.getStream());
                        bundles.add(localeBundle);
                    } catch (IOException e) {
                        log.warn("Failed to load '{}' for locale '{}' : {}", baseJcrAbsFilePath, loc, e.toString());
                    }
                }
            }
            // add default bundle as last because it is the fallback
            bundles.add(defaultBundle);

            return new CompositeResourceBundle(bundles.toArray(new ResourceBundle[bundles.size()]));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            String msg = String.format("Cannot load repository resource bundle '%s' for locale '%s'", baseJcrAbsFilePath, locale);
            throw new IllegalStateException(msg, e);
        }
    }

    static String getLocalePathForBase(final String baseJcrAbsFilePath, final Locale loc) {
        if (!baseJcrAbsFilePath.endsWith(PROPERTIES_SUFFIX) || baseJcrAbsFilePath.length() == PROPERTIES_SUFFIX_LENGTH) {
            String msg = String.format("baseJcrAbsFilePath must be of form  xxx.%s but is '%s'",PROPERTIES_SUFFIX,  baseJcrAbsFilePath);
            throw new IllegalArgumentException(msg);
        }
        final String base = baseJcrAbsFilePath.substring(0, baseJcrAbsFilePath.length() - PROPERTIES_SUFFIX_LENGTH);
        if (loc.toString().length() == 0) {
            return baseJcrAbsFilePath;
        } else {
            return base + "_" + loc.toString() + PROPERTIES_SUFFIX;
        }
    }

    /**
     * @return list of locales with most specific locale first
     */
    public static List<Locale> localeLookupList(final Locale locale) {
        List list = new ArrayList(3);
        if (locale != null) {
            list.add(locale);
            if (locale.getVariant().length() > 0) {
                list.add(new Locale(locale.getLanguage(), locale.getCountry()));
            }
            if (locale.getCountry().length() > 0) {
                list.add(new Locale(locale.getLanguage(), ""));
            }
        }
        return Collections.unmodifiableList(list);
    }
}

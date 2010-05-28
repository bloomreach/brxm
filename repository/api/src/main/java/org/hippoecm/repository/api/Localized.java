/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.api;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * The Localized object is a more generic form of the Locale object, but serves a similar purpose.
 * Where the Locale object can only hande language, country and region and only if specified in
 * that exact order, the Localized object can also indicate a locale on other factors.
 * Such as on live or preview site, or when a translation is based only upon country regardless
 * of language.
 */
public final class Localized implements Serializable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private Locale locale;

    private Localized(Locale locale) {
        this.locale = locale;
    }

    /**
     * A default non-specific localized object
     * @return a Localized object without any specification such or language or region to base a translation upon
     */
    public static Localized getInstance() {
        return new Localized(null);
    }

    /**
     * Obtain a Localized object for the specified locale
     * @param locale the Locale to base the Localized object on
     * @return the Localized object
     */
    public static Localized getInstance(Locale locale) {
        return new Localized(locale);
    }

    /**
     * Obtain a Localized object for the specified locales
     * @param locales 
     * @return the Localized object
     */
    public static Localized getInstance(Map<String, List<String>> locales) {
        List<String> list = locales.get("hippostd:language" /*HippoStdNodeType.HIPPOSTD_LANGUAGE*/); // FIXME
        if(list != null && list.size()==1)
            return getInstance(new Locale(list.get(0)));
        return getInstance();
    }

    /**
     * Returns the Locale object if available and if the Locale object holds
     * enough information to specify a translation.
     * @return returns a plain locale object, if available or null otherwise
     */
    public Locale getLocale() {
        return locale;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Localized) {
            if(locale != null) {
                return locale.equals(((Localized)other).getLocale());
            } else {
                return ((Localized)other).getLocale() == null;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.locale != null ? this.locale.hashCode() : 0);
        return hash;
    }

    /**
     * DO NOT USE THIS METHOD IS NOT PART OF THE PUBLIC API
     * @exclude
     */
    public boolean matches(Localized other) {
        Locale otherLocale = other.getLocale();
        if(locale != null && locale.getLanguage() != null)
            if(otherLocale != null && otherLocale.getLanguage() != null) {
                if(!locale.getLanguage().equals(otherLocale.getLanguage()))
                    return false;
            } else
                return false;
        if(locale != null && locale.getCountry() != null)
            if(otherLocale != null && otherLocale.getCountry() != null) {
                if(!locale.getCountry().equals(otherLocale.getCountry()))
                    return false;
            } else
                return false;
        if(locale != null && locale.getVariant() != null)
            if(otherLocale != null && otherLocale.getVariant() != null) {
                if(!locale.getVariant().equals(otherLocale.getVariant()))
                    return false;
            } else
                return false;
        return true;
    }

    /**
     * DO NOT USE THIS METHOD IS NOT PART OF THE PUBLIC API
     * @exclude
     */
    public Localized matches(Localized candidate1, Localized candidate2) {
        if(candidate1 == null) {
            if(candidate2 != null && matches(candidate2)) {
                return candidate2;
            } else {
                return null;
            }
        } else if(candidate2 == null) {
            if(matches(candidate1)) {
                return candidate1;
            } else {
                return null;
            }
        }
        Locale locale1 = candidate1.getLocale();
        Locale locale2 = candidate2.getLocale();
        if(locale != null && locale.getLanguage() != null) {
            if(locale1.getLanguage()==null) {
                if(locale2.getLanguage()!=null) {
                    return candidate2;
                }
            } else if(locale2.getLanguage()==null)
                return candidate1;
        } else {
            if(locale1.getLanguage()!=null) {
                if(locale2.getLanguage()==null) {
                    return candidate2;
                }
            } else if(locale2.getLanguage()!=null)
                return candidate1;
        }
        if(locale != null && locale.getCountry() != null) {
            if(locale1.getCountry()==null) {
                if(locale2.getCountry()!=null) {
                    return candidate2;
                }
            } else if(locale2.getCountry()==null)
                return candidate1;
        } else {
            if(locale1.getCountry()!=null) {
                if(locale2.getCountry()==null) {
                    return candidate2;
                }
            } else if(locale2.getCountry()!=null)
                return candidate1;
        }
        if(locale != null && locale.getVariant() != null) {
            if(locale1.getVariant()==null) {
                if(locale2.getVariant()!=null) {
                    return candidate2;
                }
            } else if(locale2.getVariant()==null)
                return candidate1;
        } else {
            if(locale1.getVariant()!=null) {
                if(locale2.getVariant()==null) {
                    return candidate2;
                }
            } else if(locale2.getVariant()!=null)
                return candidate1;
        }
        return candidate1;
    }

    /**
     * Gets an instance of Localized where the parameter indicates a translation from
     * which to form the localized object.  I.e. if the node is a translation node for
     * the english translation, then the returned Localized object specifies the english locale.
     * @param node the translation for which to return the Localized object
     * @return the Localized object, or null if not available
     * @throws RepositoryException
     */
    public static Localized getInstance(Node node) throws RepositoryException {
        if(node.isNodeType(HippoNodeType.NT_TRANSLATION)) {
            if(node.hasProperty(HippoNodeType.HIPPO_LANGUAGE)) {
                String language = node.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();
                if(language == null || language.trim().equals("")) {
                    return getInstance();
                } else {
                    return getInstance(new Locale(language));
                }
            } else {
                return getInstance();
            }
        } else {
            return null;
        }
    }

    /**
     * DO NOT USE THIS METHOD IS NOT PART OF THE PUBLIC API.
     * @exclude
     */
    public void setTranslation(Node node) throws RepositoryException{
        if(node.isNodeType(HippoNodeType.NT_TRANSLATION)) {
            if(locale != null) {
                node.setProperty(HippoNodeType.HIPPO_LANGUAGE, locale.getLanguage());
            } else {
                node.setProperty(HippoNodeType.HIPPO_LANGUAGE, "");
            }
        } else {
            throw new RepositoryException();
        }
    }

    public String toString() {
        return getClass().getName()+"[locale="+locale.toString()+"]";
    }
}

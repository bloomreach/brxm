/*
 *  Copyright 2009 Hippo.
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

public final class Localized implements Serializable {
    private Locale locale;

    private Localized(Locale locale) {
        this.locale = locale;
    }

    public static Localized getInstance() {
        return new Localized(null);
    }

    public static Localized getInstance(Locale locale) {
        return new Localized(locale);
    }

    public static Localized getInstance(Map<String, List<String>> locales) {
        List<String> list = locales.get("hippostd:language" /*HippoStdNodeType.HIPPOSTD_LANGUAGE*/); // FIXME auw auw auw
        if(list != null && list.size()==1)
            return getInstance(new Locale(list.get(0)));
        return getInstance();
    }

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

    public boolean matches(Localized other) {
        Locale otherLocale = other.getLocale();
        if(locale.getLanguage() != null)
            if(otherLocale.getLanguage() != null) {
                if(!locale.getLanguage().equals(otherLocale.getLanguage()))
                    return false;
            } else
                return false;
        if(locale.getCountry() != null)
            if(otherLocale.getCountry() != null) {
                if(!locale.getCountry().equals(otherLocale.getCountry()))
                    return false;
            } else
                return false;
        if(locale.getVariant() != null)
            if(otherLocale.getVariant() != null) {
                if(!locale.getVariant().equals(otherLocale.getVariant()))
                    return false;
            } else
                return false;
        return true;
    }

    public Localized matches(Localized candidate1, Localized candidate2) {
        Locale locale1 = candidate1.getLocale();
        Locale locale2 = candidate2.getLocale();
        if(candidate1 != null || !matches(candidate1)) {
            if(candidate2 != null && matches(candidate2)) {
                return candidate2;
            } else {
                return null;
            }
        } else if(candidate2 != null) {
            if(matches(candidate2)) {
                return candidate2;
            } else {
                return null;
            }
        }
        if(locale.getLanguage() != null) {
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
        if(locale.getCountry() != null) {
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
        if(locale.getVariant() != null) {
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

    public static Localized getInstance(Node node) throws RepositoryException {
        if(node.isNodeType(HippoNodeType.NT_TRANSLATION)) {
            if(node.hasProperty(HippoNodeType.HIPPO_LANGUAGE)) {
                String language = node.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();
                return getInstance(new Locale(language));
            } else
                return getInstance();
        } else
            return null;
    }

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
}

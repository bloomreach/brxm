/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.security.ldap;

public class LdapGroupSearch extends LdapUserSearch {


    public static final String PROPERTY_MEMBER_ATTR = "hippoldap:memberattribute";
    public static final String PROPERTY_MEMBERNAME_MATCHER = "hippoldap:membernamematcher";

    // matchers for substitution
    public static final String UID_MATCHER = "<uid>"; // get's replaced with uid: username
    public static final String DN_MATCHER = "<dn>"; // get's replaced with full dn: cn=username,dc=mycompany,dc=com
    public static final String COMPACT_DN_MATCHER = "<compact-dn>"; // implode attr values: username.mycompany.com
    public static final String REVERSE_COMPACT_DN_MATCHER = "<reverse-compact-dn>"; // implode attr values in rev. order: com.mycompany.username

    // defaults
    private String memberAttr = "memberUid";
    private String memberNameMatcher = UID_MATCHER;

    private static final String DEFAULT_FILTER = "(objectclass=posixGroup)";

    public LdapGroupSearch(String baseDn, String nameAttr) {
        super(baseDn, nameAttr);
        setFilter(DEFAULT_FILTER);
    }

    public String getMemberAttr() {
        return memberAttr;
    }

    public void setMemberAttr(String memberAttr) {
        this.memberAttr = memberAttr;
    }

    public String getMemberNameMatcher() {
        return memberNameMatcher;
    }

    public void setMemberNameMatcher(String memberNameMatcher) {
        this.memberNameMatcher = memberNameMatcher;
    }
}

/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.xinha.services.links;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.string.Strings;

public class ExternalXinhaLink extends XinhaLink {
    private static final long serialVersionUID = 1L;


    public static final List<String> PROTOCOLS = Arrays.asList("http://", "https://", "mailto:", "ftp://");

    String protocol;
    String address;

    public ExternalXinhaLink(Map<String, String> values) {
        super(values);

        setAddress(getHref());
        if(Strings.isEmpty(protocol)) {
            setProtocol(PROTOCOLS.get(0));
        }
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setAddress(String address) {
        this.address = address;
        for (String proto : PROTOCOLS) {
            if (address.startsWith(proto)) {
                setProtocol(proto);
                this.address = address.substring(proto.length());
                break;
            }
        }
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean isValid() {
        if (isExisting()) {
            //check protocols
            String href = getProtocol() + getAddress();
            for (String protocol : PROTOCOLS) {
                if (href.startsWith(protocol)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isExisting() {
        return !Strings.isEmpty(getAddress());
    }

    public void delete() {
        setHref(null);
    }

    public void save() {
        //never called
    }

}

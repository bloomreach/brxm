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
package org.hippoecm.hst.core.hosting;

/**
 *
 * TODO rewrite docs according latest solution as some parts have changed!!
 * 
 * 
 * The Mapping holds the relation between the requestUri and the hst2 site that needs to handle the request.
 * 
 * For example the request 
 * 
 * '/preview/home' for the host www.hippoecm.org needs to be handled by the site named 'hippoecm', but '/preview/home' 
 * for the host www.hippodocs.org needs to be handled by the site named 'hippodocs'. 
 * 
 * This can be achieved if the virtual host www.hippoecm.org has a mapping: '/preview/* --> /preview/hippoecm/${1}' and
 * for www.hippodics.org it would be '/preview/* --> /preview/hippodocs/${1}'. 
 * 
 * Typcially, the mapping '/* --> /live/hippoecm/${1}' can be used for the live for the 'hippoecm' site. Clearly, you cannot 
 * have any live urls starting with '/preview'. This can be avoided if the 'preview' can run in a different host, for example 
 * 'preview.hippoecm.org' 
 * 
 * Note that you can also have
 * 
 * '/my/strange/prefix/* --> /preview/hippoecm/${1}', where everything should just work. Obviously, all links created
 * by the hst2 framework will be prefixed with '/my/strange/prefix/' in this case. 
 * 
 * Furthermore, the <code>Mapping</code> most also be able to handle multiple sites for a single host. Then, the decision to
 * use site A instead of B is not made by the host name, but by the request uri prefix. This is useful for local development, where
 * many subsites all have to run on the same localhost and you do not want to add them all to your hosts.
 * 
 * Then for example the mapping might be:
 * 
 * '/preview/* --> /preview/${1}'
 * '/* --> /live/${1}'
 * 
 * where the subsite names are now part of the * and ${1} instead of explicitly in the mapping. 
 * 
 * Matching of mappings wrt the requestUri will be done by looping through an array of Mapping's. The prefixes with the most 
 * number of slashes should be tested first for a match, as they are more specific then prefixes with less slashes. Therefor
 * this interface extends the Comparable interface
 * 
 */

public interface Mapping extends Comparable<Mapping>{
 
    /**
     * 
     * @param pathInfo
     * @return true when this pathInfo matches the uri prefix
     */
    boolean match(String pathInfo);

    /**
     * 
     * @return the <code>VirtualHost</code> containing this Mapping
     */
    VirtualHost getVirtualHost();

    /**
     * The prefix in the url might be different then the servletpath. It might be that the url uses
     * '/p/r/e/v/i/e/w' and the servletpath is /preview. This method would return /p/r/e/v/i/e/w. 
     * @return the uri prefix as it is in the url or <code>null</code> if there should be no url prefix
     */
    String getUriPrefix();
    
    /**
     * The prefix in the url might be different then the servletpath. It might be that the url uses
     * '/p/r/e/v/i/e/w' and the servletpath is /preview. This method would return /preview
     * @return the rewritten prefix as it is in the application
     */
    String getRewrittenPrefix();
    
}

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
package org.hippoecm.hst.core.container;

/**
 * Orderable valve interface.
 * <P>
 * When an orderable valve is added into a {@link Pipeline},
 * it can be re-ordered by the execution ordering properties ('before', 'after', etc.).
 * </P>
 * <P>
 * The postrequisite valve names configured by 'before' property is the valve names that should follow this valve.
 * The prerequisite valve names configured by 'after' property is the valve names that should precede this valve.
 * The postrequisite/prerequisite names can have multiple valve names, separated by ' ', ',', '\t', '\r' or '\n'.
 * </P>
 */
public interface OrderableValve extends Valve {

    /**
     * Returns the valve name.
     * @return
     */
    public String getName();

    /**
     * Returns postrequisite valve names that should follow this valve.
     * The return can have multiple valve names, separated by ' ', ',', '\t', '\r' or '\n'
     * @return
     */
    public String getBefore();

    /**
     * Returns prerequisite valve names that should follow this valve.
     * The return can have multiple valve names, separated by ' ', ',', '\t', '\r' or '\n'
     * @return
     */
    public String getAfter();

}
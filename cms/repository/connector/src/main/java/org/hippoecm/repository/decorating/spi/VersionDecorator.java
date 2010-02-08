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
package org.hippoecm.repository.decorating.spi;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.version.Version;

import org.hippoecm.repository.decorating.DecoratorFactory;
import org.hippoecm.repository.api.HippoSession;

public class VersionDecorator extends org.hippoecm.repository.decorating.VersionDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected HippoSession remoteSession;

    protected VersionDecorator(DecoratorFactory factory, Session session, Version version) {
        super(factory, session, version);
        remoteSession = ((SessionDecorator)session).getRemoteSession();
    }

    public Node getCanonicalNode() throws RepositoryException {
        return NodeDecorator.getCanonicalNode(session, remoteSession, node);
    }

    public String getLocalizedName() throws RepositoryException {
        return NodeDecorator.getLocalizedName(session, remoteSession, node);
    }
}

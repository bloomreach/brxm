/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.repository.quartz;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.quartz.JobDetail;

public abstract class JCRJobDetail extends JobDetail {

    private static final String HIPPOSCHED_DATA = "hipposched:data";

    protected JCRJobDetail(Node jobNode, Class jobClass) throws RepositoryException {
        super(jobNode.getIdentifier(), jobClass);
    }

    public String getIdentifier() {
        return getName();
    }

    public void persist(Node node) throws RepositoryException {
        node.setProperty(HIPPOSCHED_DATA, JcrUtils.createBinaryValueFromObject(node.getSession(), this));
    }

}

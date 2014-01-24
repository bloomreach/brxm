/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.quartz.workflow;

import java.io.IOException;
import java.io.ObjectInputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.hippoecm.repository.impl.WorkflowInvocationImpl;
import org.hippoecm.repository.quartz.JCRJobDetail;
import org.hippoecm.repository.util.JcrUtils;

import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_ARGUMENTS;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_CATEGORY;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_INTERACTION;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_INTERACTION_ID;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_METHOD_NAME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_PARAMETER_TYPES;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_SUBJECT_ID;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_NAME;
import static org.hippoecm.repository.util.RepoUtils.PRIMITIVE_TO_OBJECT_TYPES;

public class WorkflowJobDetail extends JCRJobDetail {

    private WorkflowInvocation invocation;
    private String subjectId;


    public WorkflowJobDetail(Node jobNode, WorkflowInvocation invocation) throws RepositoryException {
        super(jobNode, WorkflowJob.class);
        this.invocation = invocation;
    }

    public WorkflowJobDetail(Node jobNode) throws RepositoryException {
        super(jobNode, WorkflowJob.class);
        this.subjectId = JcrUtils.getStringProperty(jobNode, HIPPOSCHED_SUBJECT_ID, null);
        final String category = JcrUtils.getStringProperty(jobNode, HIPPOSCHED_CATEGORY, null);
        final String workflowName = JcrUtils.getStringProperty(jobNode, HIPPOSCHED_WORKFLOW_NAME, null);
        final String methodName = JcrUtils.getStringProperty(jobNode, HIPPOSCHED_METHOD_NAME, null);
        final String interactionId = JcrUtils.getStringProperty(jobNode, HIPPOSCHED_INTERACTION_ID, null);
        final String interaction = JcrUtils.getStringProperty(jobNode, HIPPOSCHED_INTERACTION, null);
        final Class[] parameterTypes;
        final Property parameterTypesProperty = JcrUtils.getPropertyIfExists(jobNode, HIPPOSCHED_PARAMETER_TYPES);
        if (parameterTypesProperty != null) {
            try {
                parameterTypes = valuesToClassArray(parameterTypesProperty.getValues());
            } catch (ClassNotFoundException e) {
                throw new RepositoryException("Cannot deserialize JobDetail from node " + jobNode.getPath() + ": " + e);
            }
        } else {
            parameterTypes = new Class[] {};
        }
        final Object[] arguments;
        final Property argumentsProperty = JcrUtils.getPropertyIfExists(jobNode, HIPPOSCHED_ARGUMENTS);
        if (argumentsProperty != null) {
            try {
                arguments = valuesToObjectArray(argumentsProperty.getValues());
            } catch (IOException e) {
                throw new RepositoryException("Cannot deserialize JobDetail from node " + jobNode.getPath() + ": " + e);
            } catch (ClassNotFoundException e) {
                throw new RepositoryException("Cannot deserialize JobDetail from node " + jobNode.getPath() + ": " + e);
            }
        } else {
            arguments = new Object[] {};
        }
        this.invocation = new WorkflowInvocationImpl(category, workflowName, subjectId, methodName,
                parameterTypes, arguments, interactionId, interaction);
    }

    @Override
    public void persist(final Node node) throws RepositoryException {
        node.setProperty(HIPPOSCHED_SUBJECT_ID, invocation.getSubject().getIdentifier());
        node.setProperty(HIPPOSCHED_CATEGORY, invocation.getCategory());
        node.setProperty(HIPPOSCHED_WORKFLOW_NAME, invocation.getWorkflowName());
        node.setProperty(HIPPOSCHED_METHOD_NAME, invocation.getMethodName());
        final Class[] parameterTypes = invocation.getParameterTypes();
        if (parameterTypes != null && parameterTypes.length > 0) {
            node.setProperty(HIPPOSCHED_PARAMETER_TYPES, classArrayToStringArray(parameterTypes));
        }
        final Object[] arguments = invocation.getArguments();
        if (arguments != null && arguments.length > 0) {
            node.setProperty(HIPPOSCHED_ARGUMENTS, objectArrayToBinaryValues(arguments, node.getSession()));
        }
        node.setProperty(HIPPOSCHED_INTERACTION_ID, invocation.getInteractionId());
        node.setProperty(HIPPOSCHED_INTERACTION, invocation.getInteraction());
    }

    public String getSubjectId() {
        return subjectId;
    }

    public WorkflowInvocation getInvocation() {
        return invocation;
    }

    private static String[] classArrayToStringArray(Class[] classes) {
        final String[] result = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            if (classes[i].isPrimitive()) {
                result[i] = PRIMITIVE_TO_OBJECT_TYPES.get(classes[i]).getName();
            } else {
                result[i] = classes[i].getName();
            }
        }
        return result;
    }

    private static Value[] objectArrayToBinaryValues(Object[] objects, Session session) throws RepositoryException {
        final Value[] values = new Value[objects.length];
        for (int i = 0; i < objects.length; i++) {
            values[i] = JcrUtils.createBinaryValueFromObject(session, objects[i]);
        }
        return values;
    }

    private static Class[] valuesToClassArray(Value[] values) throws RepositoryException, ClassNotFoundException {
        final Class[] classes = new Class[values.length];
        for (int i = 0; i < values.length; i++) {
            classes[i] = Class.forName(values[i].getString());
        }
        return classes;
    }

    private static Object[] valuesToObjectArray(Value[] values) throws RepositoryException, IOException, ClassNotFoundException {
        final Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(values[i].getBinary().getStream());
                objects[i] = ois.readObject();
            } finally {
                IOUtils.closeQuietly(ois);
            }
        }
        return objects;
    }

}

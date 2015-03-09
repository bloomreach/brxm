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
package org.onehippo.repository.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.onehippo.cms7.event.HippoEvent;

public class HippoWorkflowEvent<T extends HippoWorkflowEvent<T>> extends HippoEvent<T> {

    private static final String CLASS_NAME = "className";
    /**
     * @deprecated use {@link #ACTION} instead
     */
    private static final String METHOD_NAME = "methodName";
    private static final String SUBJECT_ID = "subjectId";
    /**
     * @deprecated use {@link #SUBJECT_ID} instead
     */
    private static final String HANDLE_UUID = "handleUuid";
    private static final String SUBJECT_PATH = "subjectPath";
    /**
     * @deprecated use {@link #SUBJECT_PATH} instead
     */
    private static final String DOCUMENT_PATH = "documentPath";
    private static final String RETURN_VALUE = "returnValue";
    private static final String RETURN_TYPE = "returnType";
    private static final String ARGUMENTS = "arguments";
    private static final String INTERACTION_ID = "interactionId";
    private static final String INTERACTION = "interaction";
    private static final String WORKFLOW_CATEGORY = "workflowCategory";
    private static final String WORKFLOW_NAME = "workflowName";
    private static final String EXCEPTION = "exception";
    private static final String DOCUMENT_TYPE = "documentType";

    public HippoWorkflowEvent() {
        super("repository");
        category("workflow");
    }

    public HippoWorkflowEvent(HippoEvent event) {
        super(event);
    }

    /**
     * The name of the workflow class
     */
    public String className() {
        return get(CLASS_NAME);
    }

    public T className(String className) {
        return set(CLASS_NAME, className);
    }

    /**
     * @deprecated use {@link #action()} instead
     */
    @Deprecated
    public String methodName() {
        return get(METHOD_NAME);
    }

    /**
     * @deprecated use {@link #action(String)} instead
     */
    @Deprecated
    public T methodName(String methodName) {
        return set(METHOD_NAME, methodName);
    }

    /**
     * The JCR node identifier of the workflow subject
     */
    public String subjectId() {
        return get(SUBJECT_ID);
    }

    public T subjectId(String subjectId) {
        return set(SUBJECT_ID, subjectId);
    }

    /**
     * @deprecated use {@link #subjectId()} instead
     */
    @Deprecated
    public String handleUuid() {
        return get(HANDLE_UUID);
    }

    /**
     * @deprecated use {@link #subjectId(String)} instead
     */
    @Deprecated
    public T handleUuid(String handleUuid) {
        return set(HANDLE_UUID, handleUuid);
    }

    /**
     * The java type of the object returned by the workflow action, if any
     */
    public String returnType() {
        return get(RETURN_TYPE);
    }

    public T returnType(String returnType) {
        return set(RETURN_TYPE, returnType);
    }

    /**
     * The value returned by the workflow action, if any
     */
    public String returnValue() {
        return get(RETURN_VALUE);
    }

    public T returnValue(String returnValue) {
        return set(RETURN_VALUE, returnValue);
    }

    /**
     * The JCR node path of the workflow subject
     */
    public String subjectPath() {
        return get(SUBJECT_PATH);
    }

    public T subjectPath(String subjectPath) {
        return set(SUBJECT_PATH, subjectPath);
    }

    /**
     * @deprecated use {@link #subjectPath()} instead
     */
    @Deprecated
    public String documentPath() {
        return get(DOCUMENT_PATH);
    }

    /**
     * @deprecated use {@link #subjectId(String)} instead
     */
    @Deprecated
    public T documentPath(String documentPath) {
        return set(DOCUMENT_PATH, documentPath);
    }

    /**
     * The arguments passed to the workflow invocation
     */
    public List<String> arguments() {
        List<String> arguments =  get(ARGUMENTS);
        if (arguments != null) {
            return Collections.unmodifiableList(arguments);
        }
        return null;
    }

    public T arguments(List<String> arguments) {
        return set(ARGUMENTS, new ArrayList<String>(arguments));
    }

    /**
     * A workflow invocation can result in nested workflow invocations if the workflow action
     * in turn invokes another workflow. In this case these workflow actions have the same interaction id
     * in order to identify them as part of the same user-level workflow action.
     */
    public String interactionId() {
        return get(INTERACTION_ID);
    }

    /**
     * The value of the interaction property is a combination of the category, name, and action of the root
     * invocation of the invocation hierarchy.
     */
    public String interaction() {
        return get(INTERACTION);
    }

    public T interactionId(String interactionId) {
        return set(INTERACTION_ID, interactionId);
    }

    public T interaction(String interaction) {
        return set(INTERACTION, interaction);
    }

    /**
     * The workflow category
     */
    public String workflowCategory() {
        return get(WORKFLOW_CATEGORY);
    }

    public T workflowCategory(String category) {
        return set(WORKFLOW_CATEGORY, category);
    }

    /**
     * The workflow name
     */
    public String workflowName() {
        return get(WORKFLOW_NAME);
    }

    public T workflowName(String workflowName) {
        return set(WORKFLOW_NAME, workflowName);
    }

    /**
     * The exception if any occurred during the invocation of the workflow
     */
    public Throwable exception() {
        return get(EXCEPTION);
    }

    public T exception(Throwable e) {
        return set(EXCEPTION, e);
    }

    /**
     * Whether the workflow invocation was successful
     */
    public Boolean success() {
        return get(EXCEPTION) == null;
    }

    /**
     * The type of document the workflow was called on, if any
     */
    public String documentType() {
        return get(DOCUMENT_TYPE);
    }

    public T documentType(String documentType) {
        return set(DOCUMENT_TYPE, documentType);
    }
}

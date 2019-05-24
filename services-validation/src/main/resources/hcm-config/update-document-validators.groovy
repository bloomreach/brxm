/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.cms.admin.updater

import org.onehippo.repository.update.BaseNodeUpdateVisitor

import javax.jcr.Node
import javax.jcr.RepositoryException
import javax.jcr.Session

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYSEDIT_TYPE
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VALIDATORS

class UpdateDocumentValidatorsConfiguration extends BaseNodeUpdateVisitor {

    boolean logSkippedNodePaths() {
        return false // don't log skipped node paths
    }

    boolean skipCheckoutNodes() {
        return false // return true for readonly visitors and/or updates unrelated to versioned content
    }

    Node firstNode(final Session session) throws RepositoryException {
        return null // implement when using custom node selection/navigation
    }

    Node nextNode() throws RepositoryException {
        return null // implement when using custom node selection/navigation
    }

    boolean doUpdate(Node node) {
        if (!node.hasProperty(HIPPO_VALIDATORS)) {
            return false
        }

        def logLines = []
        def validatorsOriginal = readValidators(node)
        logLines << "Existing validators          : ${validatorsOriginal.toString()}"

        // replace resource-required -> required
        def validators = replaceValidator(validatorsOriginal, 'resource-required', 'required')
        logLines << "Without resource-required    : ${validators.toString()}"

        // fix required & non-empty combination
        validators = removeNonEmptyValidator(validators)
        logLines << "Without non-empty if required: ${validators.toString()}"

        // log warning for non-empty only
        logNonEmptyWarning(validators, node.path)

        // fix non-empty for Html fields
        if (!node.getProperty(HIPPOSYSEDIT_TYPE)) {
            log.warn "Field node with validators but without type! Cannot check this node: ${node.path}"
        } else {
            def type = node.getProperty(HIPPOSYSEDIT_TYPE).string
            if (type == 'Html') {
                validators = replaceValidator(validators, 'non-empty', 'non-empty-html')
                logLines << "Fixed html fields            : ${validators.toString()}"
            }
        }

        // set the property
        if (!validatorsOriginal.equals(validators)) {
            log.debug "Updating node ${node.path}"
            logLines.each { log.debug "${it}" }
            String[] strings = validators.toArray(new String[0])
            node.setProperty(HIPPO_VALIDATORS, strings)
            return true
        }

        // Uncomment these lines for more logging
        // log.debug "Unchanged node ${node.path}"
        // logLines.each{ log.debug "${it}" }
        return false
    }

    static List<String> readValidators(Node node) {
        def validatorsProperty = node.getProperty(HIPPO_VALIDATORS)
        def values = validatorsProperty.getValues()
        def validators = []
        values.each { validators << it.string }
        return validators
    }

    static List<String> replaceValidator(validators, target, replacement) {
        return validators.collect { item -> item == target ? replacement : item }.unique()
    }

    static List<String> removeNonEmptyValidator(validators) {
        if (validators.contains('required') && validators.contains('non-empty')) {
            validators.remove('non-empty')
        }
        return validators
    }

    void logNonEmptyWarning(validators, path) {
        if (validators.contains('non-empty') && !validators.contains('required') && !validators.contains('optional')) {
            log.info "Field with a 'non-empty' validator found that is not 'required' nor 'optional'. Consider using 'required' or 'optional' instead. See the field definition at node ${path}"
        }
    }

    boolean undoUpdate(Node node) {
        throw new UnsupportedOperationException('Updater does not implement undoUpdate method')
    }
}

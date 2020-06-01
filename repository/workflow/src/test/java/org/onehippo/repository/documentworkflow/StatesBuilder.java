/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.documentworkflow;

import java.util.TreeSet;

public class StatesBuilder {

    public static String STATE_NO_DOCUMENT = "no-document";
    public static String STATE_STATUS = "status";
    public static String STATE_NO_EDIT = "no-edit";
    public static String STATE_EDITING = "editing";
    public static String STATE_EDITABLE = "editable";
    public static String STATE_NO_REQUEST = "no-request";
    public static String STATE_REQUESTED = "requested";
    public static String STATE_NO_PUBLISH = "no-publish";
    public static String STATE_PUBLISHABLE = "publishable";
    public static String STATE_NO_DEPUBLISH = "no-depublish";
    public static String STATE_DEPUBLISHABLE = "depublishable";
    public static String STATE_NO_VERSIONING = "no-versioning";
    public static String STATE_VERSIONABLE = "versionable";
    public static String STATE_NO_TERMINATE = "no-terminate";
    public static String STATE_TERMINATEABLE = "terminateable";
    public static String STATE_NO_COPY = "no-copy";
    public static String STATE_COPYABLE = "copyable";
    public static String STATE_BRANCHABLE = "branchable";
    public static String STATE_NO_BRANCHABLE = "no-branchable";
    public static String STATE_NO_CHECKOUT_BRANCH = "no-checkout-branch";
    public static String STATE_CAN_CHECKOUT_BRANCH = "can-checkout-branch";
    public static String STATE_NO_REMOVE_BRANCH = "no-remove-branch";
    public static String STATE_CAN_REMOVE_BRANCH = "can-remove-branch";
    public static String STATE_NO_REINTEGRATE_BRANCH = "no-reintegrate-branch";
    public static String STATE_CAN_REINTEGRATE_BRANCH = "can-reintegrate-branch";
    public static String STATE_NO_PUBLISH_BRANCH = "no-publish-branch";
    public static String STATE_CAN_PUBLISH_BRANCH = "can-publish-branch";
    public static String STATE_NO_DEPUBLISH_BRANCH = "no-depublish-branch";
    public static String STATE_CAN_DEPUBLISH_BRANCH = "can-depublish-branch";
    public static String STATE_LOGEVENT = "logEvent";

    private TreeSet<String> states = new TreeSet<>();

    public static StatesBuilder build() {
        return new StatesBuilder();
    }

    public TreeSet<String> states() {
        return states;
    }

    public StatesBuilder noDocument() {
        states.add(STATE_NO_DOCUMENT);
        return this;
    }

    public StatesBuilder status() {
        states.add(STATE_STATUS);
        return this;
    }

    public StatesBuilder noEdit() {
        states.add(STATE_NO_EDIT);
        return this;
    }

    public StatesBuilder editing() {
        states.add(STATE_EDITING);
        return this;
    }

    public StatesBuilder editable() {
        states.add(STATE_EDITABLE);
        return this;
    }

    public StatesBuilder noRequest() {
        states.add(STATE_NO_REQUEST);
        return this;
    }

    public StatesBuilder requested() {
        states.add(STATE_REQUESTED);
        return this;
    }

    public StatesBuilder noPublish() {
        states.add(STATE_NO_PUBLISH);
        return this;
    }

    public StatesBuilder publishable() {
        states.add(STATE_PUBLISHABLE);
        return this;
    }

    public StatesBuilder noDepublish() {
        states.add(STATE_NO_DEPUBLISH);
        return this;
    }

    public StatesBuilder depublishable() {
        states.add(STATE_DEPUBLISHABLE);
        return this;
    }

    public StatesBuilder noVersioning() {
        states.add(STATE_NO_VERSIONING);
        return this;
    }

    public StatesBuilder versionable() {
        states.add(STATE_VERSIONABLE);
        return this;
    }

    public StatesBuilder noTerminate() {
        states.add(STATE_NO_TERMINATE);
        return this;
    }

    public StatesBuilder terminateable() {
        states.add(STATE_TERMINATEABLE);
        return this;
    }

    public StatesBuilder noCopy() {
        states.add(STATE_NO_COPY);
        return this;
    }

    public StatesBuilder copyable() {
        states.add(STATE_COPYABLE);
        return this;
    }

    public StatesBuilder logEvent() {
        states.add(STATE_LOGEVENT);
        return this;
    }

    public StatesBuilder branchable() {
        states.add(STATE_BRANCHABLE);
        return this;
    }

    public StatesBuilder noBranchable() {
        states.add(STATE_NO_BRANCHABLE);
        return this;
    }

    public StatesBuilder noCheckoutBranch() {
        states.add(STATE_NO_CHECKOUT_BRANCH);
        return this;
    }
    public StatesBuilder canCheckoutBranch() {
        states.add(STATE_CAN_CHECKOUT_BRANCH);
        return this;
    }

    public StatesBuilder noRemoveBranch() {
        states.add(STATE_NO_REMOVE_BRANCH);
        return this;
    }
    public StatesBuilder canRemoveBranch() {
        states.add(STATE_CAN_REMOVE_BRANCH);
        return this;
    }

    public StatesBuilder noReintegrateBranch() {
        states.add(STATE_NO_REINTEGRATE_BRANCH);
        return this;
    }
    public StatesBuilder canReintegrateBranch() {
        states.add(STATE_CAN_REINTEGRATE_BRANCH);
        return this;
    }

    public StatesBuilder noPublishBranch() {
        states.add(STATE_NO_PUBLISH_BRANCH);
        return this;
    }
    public StatesBuilder canPublishBranch() {
        states.add(STATE_CAN_PUBLISH_BRANCH);
        return this;
    }

    public StatesBuilder noDepublishBranch() {
        states.add(STATE_NO_DEPUBLISH_BRANCH);
        return this;
    }
    public StatesBuilder canDepublishBranch() {
        states.add(STATE_CAN_DEPUBLISH_BRANCH);
        return this;
    }


}

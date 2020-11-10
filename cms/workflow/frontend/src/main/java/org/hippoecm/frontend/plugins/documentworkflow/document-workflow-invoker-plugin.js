/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

(function () {
  "use strict";

  window.Hippo = window.Hippo || {};
  window.Hippo.Workflow = window.Hippo.Workflow || {
    _active: null,
    _payload: null,
  };

  Hippo.Workflow.invoke = function(documentId, category, action) {
    const CALLBACK_URL = '${callbackUrl}';

    return new Promise((resolve, reject) => {
      Hippo.Workflow._active = { resolve, reject };
      Wicket.Ajax.get({
        u: CALLBACK_URL,
        ep: {
          documentId,
          category,
          action,
        },
        fh: [(settings, xhr, {message}) => Hippo.Workflow.reject(message)],
      });
    });
};

  Hippo.Workflow.resolve = function(payload) {
    if (!Hippo.Workflow._active) {
      return;
    }

    const { resolve } = Hippo.Workflow._active;
    delete Hippo.Workflow._active;

    payload = Hippo.Workflow._payload || payload;
    delete Hippo.Workflow._payload;

    resolve(payload);
  }

  Hippo.Workflow.reject = function(payload) {
    if (!Hippo.Workflow._active) {
      return;
    }

    const { reject } = Hippo.Workflow._active;
    delete Hippo.Workflow._active;

    payload = Hippo.Workflow._payload || payload;
    delete Hippo.Workflow._payload;

    reject(payload);
  }

  Hippo.Workflow.setPayload = function(payload) {
    Hippo.Workflow._payload = payload;
  }

}());
//# sourceURL=document-workflow-invoker-plugin.js

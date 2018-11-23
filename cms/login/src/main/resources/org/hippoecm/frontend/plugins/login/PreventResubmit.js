/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function(window, $) {
  "use strict";

  window.Hippo = window.Hippo || {};

  if (!Hippo.PreventResubmit) {

    Hippo.PreventResubmit = function(formSelector) {
      var el = $(formSelector);
      if (el === null || el.length === 0) {
        console.warn("Cannot find form element '" + formSelector + "' to prevent resubmit behavior");
      } else {
        el.submitting = false;
        el.submit(function (e) {
          if (el.submitting) {
            e.preventDefault();
          } else {
            $(':submit', el).prop('disabled', true);
            el.submitting = true;
          }
        });
      }
    };
  }
}(window, jQuery));

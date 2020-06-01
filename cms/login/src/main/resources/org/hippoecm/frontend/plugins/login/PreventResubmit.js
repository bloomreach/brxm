/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
      var form = $(formSelector);
      if (form === null || form.length === 0) {
        console.warn("Cannot find form element '" + formSelector + "' to prevent resubmit behavior");
      } else {
        form.submitting = false;
        form.submit(function (e) {
          if (form.submitting) {
            e.preventDefault();
          } else {
            form.addClass('form-disabled');
            // disable the submit button
            $(':submit', form).prop('disabled', true);
            // set all form fields to readonly so that they can't be changed but their values are still submitted
            $(':input', form).prop('readonly', true);
            // select element do no support the readonly attribute, but we can disable all option elements
            // that are not selected which prevents the user from selecting a different option
            $('option:not(:selected)', form).attr('disabled',true);
            form.submitting = true;
          }
        });
      }
    };
  }
}(window, jQuery));

/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function ($) {

  var textArea = $('#${markupId}');
  if (textArea.length === 0) {
    return;
  }
  CodeMirror.fromTextArea(textArea[0], {
    lineNumbers: true,
    matchBrackets: true,
    mode: '${editorMode}',
    editorName: '${editorName}',
    readOnly: ${readOnly},
    onBlur: function (codeMirror) {
      codeMirror.save();
      if (${changeEventTriggerEnabled}) {
        textArea.trigger($.Event('change', {
          'bubbles': false,
          'cancelable': true
        }));
      }
    }
  });

}(jQuery));

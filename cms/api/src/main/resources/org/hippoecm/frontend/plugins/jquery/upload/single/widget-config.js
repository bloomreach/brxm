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

/* jshint undef:true */
/* global $, window */

(function () {
  'use strict';

  $('#${componentMarkupId}').fileupload({
    autoUpload: ${autoUpload},
    url: '${url}',
    maxNumberOfFiles: 1,
    maxFileSize: ${max.file.size},
    acceptFileTypes: /(\.)(${acceptFileTypes})$/i,
    dataType: 'json',

    done: function (e, data) {
      var that = $(this).data('blueimp-fileupload') || $(this).data('fileupload'),
        files = that.getFilesFromResponse(data);
      if (data.context) {
        data.context.each(function (index) {
          var file = files[index] || {error: 'Empty file upload result'};
          if (file.error) {
            console.error(file.error)
          }
        });
      }

      // a wicket ajax callback to refresh container
      ${callbackRefreshScript}
    }
  }).bind('fileuploadadd', function (e, data) {
    var widget = $('#${componentMarkupId}').data("blueimp-fileupload");
    if (data && data.files && data.files.length > 0) {
      showSelectedFile(data.files[0].name);
      // store selected file
      widget.uploaddata = data;

      // notify server on file-change event
      ${callbackFileOnChangeScript}
    }
  }).bind('fileuploadsubmit', function (e, data) {
    var widget = $('#${componentMarkupId}').data("blueimp-fileupload");
    // remove stored file that has been uploaded
    widget.uploaddata = null;

    clearSelectedFile();
  });
})();

function showSelectedFile (filename) {
  $('#${componentMarkupId}').find('#selected-file').text(filename);
}

/**
 * Clear the span element displaying selected file
 */
function clearSelectedFile () {
  $('#${componentMarkupId}').find('#selected-file').text('');
}
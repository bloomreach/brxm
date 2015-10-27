/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

/* global $, window */

(function () {
  'use strict';

  /**
   * Notifies Wicket that the selection changed
   */
  function notifySelectionChange() {
    var url = '${selectionChangeUrl}';
    Wicket.Ajax.get( {
      u: url + "&numberOfFiles=" + this.getNumberOfFiles()
    });
  }

  /**
   * Notifies Wicket that all files have been uploaded
   */
  function notifyUploadDone(numberOfFiles, error) {
    var url = '${fileUploadDoneUrl}';
    Wicket.Ajax.get( {
      u: url + "&numberOfFiles=" + numberOfFiles + "&error=" + error
    });
  }

  $('#${componentMarkupId}').fileupload({
    autoUpload: false,
    url: '${url}',
    onUploadDone: notifyUploadDone,
    onSelectionChange: notifySelectionChange,
    maxNumberOfFiles: ${maxNumberOfFiles},
    maxFileSize: ${max.file.size},
    acceptFileTypes: /(\.)(${acceptFileTypes})$/i,
    dataType: 'json',
    previewMaxWidth: 32,
    previewMaxHeight: 32
  });
})();


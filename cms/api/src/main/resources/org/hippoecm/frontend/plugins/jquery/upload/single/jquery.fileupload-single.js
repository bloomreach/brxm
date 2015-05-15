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

/* global define, window */

(function (factory) {
  'use strict';
  if (typeof define === 'function' && define.amd) {
    // Register as an anonymous AMD module:
    define([
      'jquery',
      './jquery.fileupload-process'
    ], factory);
  } else {
    // Browser globals:
    factory(
      window.jQuery
    );
  }
}(function ($) {
  'use strict';
  // does not support client side validation
  $.blueimp.fileupload.prototype.options.processQueue = [];

  $.widget('blueimp.fileupload', $.blueimp.fileupload, {
    getFilesFromResponse: function (data) {
      if (data.result && $.isArray(data.result.files)) {
        return data.result.files;
      }
      return [];
    },

    uploadFile: function () {
      if (this.uploaddata && this.uploaddata.submit) {
        this.uploaddata.submit();
      }
    }
  });
}));

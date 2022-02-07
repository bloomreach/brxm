/*
 * Copyright 2015-2021 Hippo B.V. (http://www.onehippo.com)
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

/* jshint nomen:false */
/* jshint undef:true */
/* global define, window, Wicket */

/* jquery fileupload widget using in hippo image gallery dialog */

(function (factory) {
  'use strict';
  if (typeof define === 'function' && define.amd) {
    // Register as an anonymous AMD module:
    define([
      'jquery',
      './jquery.fileupload-ui'
    ], factory);
  } else {
    // Browser globals:
    factory(
      window.jQuery
    );
  }
}(function ($) {
  'use strict';
  var originalStart = $.blueimp.fileupload.prototype.options.start;
  var originalStop = $.blueimp.fileupload.prototype.options.stop;
  var originalDone = $.blueimp.fileupload.prototype.options.done;
  var originalAdd = $.blueimp.fileupload.prototype.options.add;

  $.widget('blueimp.fileupload', $.blueimp.fileupload, {
    numberOfCompletedFiles: 0,
    hasError: false,
    options: {
      // disable default max-number-of-files validation in jquery.fileupload-validation
      maxNumberOfFiles: null,

      onSelectionChange: function () {
      },

      failed: function () {
        // event called when removing a file to the selection list
        var that = $(this).data('blueimp-fileupload') || $(this).data('fileupload'),
          numberOfValidFiles = that.options.getNumberOfValidFiles(),
          numberOfSelectedFiles = that.options.getNumberOfSelectedFiles();

        if (numberOfSelectedFiles <= that.options.maxTotalFiles) {
          that._clearErrorMessage();
        }
        that.options.onSelectionChange(numberOfValidFiles, numberOfSelectedFiles);
      },

      add: function (e, data) {
        // event called when adding a file to the selection list
        originalAdd.call(this, e, data);

        var that = $(this).data('blueimp-fileupload') || $(this).data('fileupload'),
          numberOfValidFiles = that.options.getNumberOfValidFiles(),
          numberOfSelectedFiles = that.options.getNumberOfSelectedFiles();

        if (numberOfSelectedFiles > that.options.maxTotalFiles) {
          that._showErrorMessage(that.options.i18n('maxNumberOfFilesWidget'));
        }

        that.options.onSelectionChange(numberOfValidFiles, numberOfSelectedFiles);
      },

      start: function (e) {
        var that = $(this).data('blueimp-fileupload') || $(this).data('fileupload');
        that.disable();
        originalStart.call(this, e);
        that._showProgressBar();
      },

      stop: function (e) {
        originalStop.call(this, e);
        var that = $(this).data('blueimp-fileupload') || $(this).data('fileupload');
        that._hideProgressBar();
      },

      always: function (e, data) {
        var that = $(this).data('blueimp-fileupload') || $(this).data('fileupload');
        that.enable();
        // sorting error uploading files to top
        if (that.hasError) {
          that.element.find('.files').prepend(that.element.find('.files .error').parents('tr'));
        }
      },

      done: function (e, data) {
        originalDone.call(this, e, data);
        // this event is fired after each file uploading is sent
        var that = $(this).data('blueimp-fileupload') || $(this).data('fileupload'),
          numberOfValidFiles = that.options.getNumberOfValidFiles();

        if (data.result.files && data.result.files.length) {
          that.numberOfCompletedFiles += data.result.files.length;

          if (!that.hasError) {
            that.hasError = data.result.files.some(function (file) {
              return !!file.error;
            });
          }
        }

        if (that.numberOfCompletedFiles >= numberOfValidFiles) {
          that.options.onUploadDone(that.numberOfCompletedFiles, that.hasError);
        }
      },

      /**
       * Return number of valid files to upload
       * @returns {number}
       */
      getNumberOfValidFiles: function () {
        var selectedFiles = this.filesContainer.children();
        return selectedFiles.length - selectedFiles.find('.error').not(':empty').length;
      },

      /**
       * Return all selected files, including processing ones
       */
      getNumberOfSelectedFiles: function () {
        return this.filesContainer.children().length;
      }
    },

    _showProgressBar: function () {
      this.element.find('.progress').addClass('visible');
    },

    _hideProgressBar: function () {
      this.element.find('.progress').removeClass('visible');
    },

    _showErrorMessage: function (error) {
      this.element.find('.fileupload-error').text(error).show();
    },

    _clearErrorMessage: function () {
      this.element.find('.fileupload-error').text('').hide();
    },

    /**
     * Invoke this method to upload all selected files
     */
    uploadFiles: function () {
      var filesList = this.options.filesContainer;

      if (filesList.children().length > this.options.maxTotalFiles) {
        // prevent user from uploading too many files
        return;
      }
      this.element.closest('form').find('input[type=submit]').prop('disabled', true);

      $.each(filesList.children(), function (idx, template) {
        var data = $.data(template, 'data');
        // don't submit invalid files
        if (data && data.files && !data.files.error && data.submit) {
          data.submit();
        }
      });
    }
  });
}));

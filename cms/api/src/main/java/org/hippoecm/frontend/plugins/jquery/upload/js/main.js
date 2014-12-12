/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

jqueryFileUploadImpl = {
    filelist: {},
    fileuploadWidget: {},
    init: function () {
        console.log('init jquery fileupload');
        // clean the file list
        this.filelist = {};
        this.fileuploadWidget = $('#${componentMarkupId}').fileupload({
            url: '${url}',
            maxNumberOfFiles: ${maxNumberOfFiles},
            maxFileSize: ${max.file.size},
            acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
            dataType: 'json',
            previewMaxWidth: 32,
            previewMaxHeight: 32,
            imageCrop: true, // Force cropped images
            process: [
                {
                    action: 'load',
                    fileTypes: /^image\/(gif|jpeg|png)$/,
                    maxFileSize: ${max.file.size}
                },
                {
                    action: 'resize',
                    maxWidth: ${max.width},
                    maxHeight: ${max.height}
                },
                {
                    action: 'save'
                }
            ]
        }).bind('fileuploadcompleted', function (e, data) {
            var filesContainer = $(this).find('.files');
            // remove all 'template-download' rows after uploading
            filesContainer.find('.template-upload').remove();
            console.log("fileuploadcompleted");
        }).bind('fileuploaddone', function (e, data) {
            var wcall = Wicket.Ajax.get({ 'u': '${fileUploadDoneUrl}'});
            console.log('fileuploaddone : ' + wcall);
        }).bind('fileuploadchange', function (e, data) {
            var fileNames = [];
            $.each(data.files, function (idx, file) {
                fileNames.push(file.name);
                jqueryFileUploadImpl.filelist[file.name] = file;
            });
            console.log("fileuploadchange:%s", fileNames.join());
        }).bind('fileuploadfail', function (e, data) {
            var fileNames = [];
            $.each(data.files, function (idx, file) {
                fileNames.push(file.name);
                delete jqueryFileUploadImpl.filelist[file.name];
            });
            console.log("fileuploadfail:%s", fileNames.join());
        }).bind('fileuploadprocessfail', function (e, data) {
            var fileNames = [];
            $.each(data.files, function (idx, file) {
                fileNames.push(file.name);
                delete jqueryFileUploadImpl.filelist[file.name];
            });
            console.log('fileuploadprocessfail:%s', fileNames.join());
        });
    },
    // upload all files in the container in a single POST request
    uploadFiles: function () {
        var uploadfiles = [];
        for (var filename in this.filelist) {
            uploadfiles.push(this.filelist[filename]);
        }
        // disable inputs
        $('#${componentMarkupId}').find('input').prop('disabled', true);
        console.log("Total uploading files: %s", uploadfiles.join());
        this.fileuploadWidget.fileupload('send', {files: uploadfiles});
    }
};
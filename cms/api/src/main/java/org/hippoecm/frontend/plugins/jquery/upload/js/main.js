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
    numberOfCompletedFiles: 0,
    hasError: false,
    fileuploadWidget: {},
    init: function () {
        console.log('init jquery fileupload');
        this.hasError = false;
        this.numberOfCompletedFiles = 0;

        this.fileuploadWidget = $('#${componentMarkupId}').fileupload({
            url: '${url}',
            maxNumberOfFiles: ${maxNumberOfFiles},
            maxFileSize: ${max.file.size},
            acceptFileTypes: /(\.|\/)(${acceptFileTypes})$/i,
            dataType: 'json',
            previewMaxWidth: 32,
            previewMaxHeight: 32
        }).bind('fileuploaddone', function (e, data) {
            // this event is fired after each file uploading is sent
            var widget = $('#${componentMarkupId}').fileupload("instance"),
                    filesList = widget.options.filesContainer,
                    isError = false,
                    numberOfSentFiles = Math.min(filesList.children().length, widget.options.maxNumberOfFiles);

            if (data.result.files && data.result.files.length) {
                jqueryFileUploadImpl.numberOfCompletedFiles += data.result.files.length;
            } else {
                console.error('Invalid jquery fileupload response from server');
            }
            console.log('Complete %s/%s', jqueryFileUploadImpl.numberOfCompletedFiles, numberOfSentFiles);
            $.each(data.result.files, function (idx, file) {
                if (file.error) {
                    console.error('uploading error: %s', file.error);
                    isError = true;
                }
            });

            if (!jqueryFileUploadImpl.hasError && isError) {
                jqueryFileUploadImpl.hasError = true;
            }
            if (jqueryFileUploadImpl.numberOfCompletedFiles >= numberOfSentFiles) {
                if (jqueryFileUploadImpl.hasError) {
                    $('#${componentMarkupId} .files').prepend($('#${componentMarkupId} .files .error').parents('tr'));
                } else {
                    Wicket.Window.get().close();
                }
            }
        });
    },
    // upload all files in the container simultaneously, each in a POST request
    uploadFiles: function () {
        var widget = $('#${componentMarkupId}').fileupload("instance"),
            filesList = widget.options.filesContainer,
            numberOfSentFiles = 0;

        $.each(filesList.children(), function (idx, template) {
            if (numberOfSentFiles < widget.options.maxNumberOfFiles) {
                var data = $.data(template, 'data');
                if (data && data.submit) {
                    console.log("uploading #%s", idx);
                    data.submit();
                    numberOfSentFiles++;
                }
            }
        });
        // disable inputs
        $('#${componentMarkupId}').find('input').prop('disabled', true);

        this.notifyUpload(numberOfSentFiles);
    },
    /**
     * Notify server on number of uploading files. The message format:
     * {
     *  "total" : numberOfFiles
     * }
     * Expected to receive:
     * {
     *  "status" : "OK" | "FAILED"
     * }
     *
     * @param numberOfFiles
     */
    notifyUpload: function (numberOfFiles){
        var notificationData = {};
        notificationData.total = numberOfFiles;
        console.log("total uploaded: %s", numberOfFiles);

        $.ajax({
            url: '${fileUploadDoneUrl}',
            type: 'POST',
            contentType: 'application/json; charset=utf-8',
            cache: false,
            dataType: 'json',
            data: JSON.stringify(notificationData),
            success: function (json) {
                if (json.status === 'OK') {
                    console.log('Sent notification successfully');
                } else {
                    console.error('Failed to send notification');
                }
            },
            error : function (XMLHttpRequest, textStatus, errorThrow) {
                console.error("error response: %s; %s; %s", XMLHttpRequest.responseText, textStatus, errorThrow);
            }
        });
    }
};
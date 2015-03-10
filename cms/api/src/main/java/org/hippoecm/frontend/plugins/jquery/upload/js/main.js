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
                    numberOfFiles = filesList.children().length;

            if (data.result.files && data.result.files.length) {
                jqueryFileUploadImpl.numberOfCompletedFiles += data.result.files.length;
            }
            $.each(data.result.files, function (idx, file) {
                if (file.error) {
                    console.error('uploading error: %s', file.error);
                    jqueryFileUploadImpl.hasError = true;
                }
            });

            if (jqueryFileUploadImpl.numberOfCompletedFiles === numberOfFiles && !jqueryFileUploadImpl.hasError) {
                Wicket.Window.get().close();
            }
        }).bind('fileuploadalways', function (e, data) {
            $('#${componentMarkupId}').find('button').prop('disabled',true);
            if (jqueryFileUploadImpl.hasError) {
                $('#${componentMarkupId} .files').prepend($('#${componentMarkupId} .files .error').parents('tr'));
            }
        });
    },

    uploadFiles: function () {
        var widget = $('#${componentMarkupId}').fileupload("instance"),
                filesList = widget.options.filesContainer,
                numberOfSentFiles = 0;

        if (filesList.children().length > widget.options.maxNumberOfFiles) {
            $('#${componentMarkupId} .fileupload-process').text(widget.options.i18n('maxNumberOfFiles')).show();
            return;
        }
        $('#${componentMarkupId} .fileupload-process').hide();
        $('#${componentMarkupId}').closest('form').find('input[type=submit]').prop('disabled',true);

        $.each(filesList.children(), function (idx, template) {
            var data = $.data(template, 'data');
            if (data && data.submit) {
                data.submit();
                numberOfSentFiles++;
            }
        });

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

        $.ajax({
            url: '${fileUploadDoneUrl}',
            type: 'POST',
            contentType: 'application/json; charset=utf-8',
            cache: false,
            dataType: 'json',
            data: JSON.stringify(notificationData),
            success: function (json) {
                if (json.status !== 'OK') {
                    console.error('Failed to send notification');
                }
            },
            error : function (XMLHttpRequest, textStatus, errorThrow) {
                console.error("error response: %s; %s; %s", XMLHttpRequest.responseText, textStatus, errorThrow);
            }
        });
    }
};
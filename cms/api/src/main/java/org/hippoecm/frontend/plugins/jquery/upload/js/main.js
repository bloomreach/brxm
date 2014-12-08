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

function loadJQueryFileUpload(){
    $(function () {
        'use strict';

        // Initialize the jQuery File Upload widget:
        $('#${componentMarkupId}').fileupload({
            url: '${url}',
            maxFileSize: ${max.file.size},
            acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
            dataType: 'json',
            imageMaxWidth: 512,
            imageMaxHeight: 512,
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
        }).bind('fileuploaddone', function(e, data){
            var wcall = Wicket.Ajax.get({ 'u': '${fileuploaddoneUrl}'});
            console.log('fileuploaddone : ' + wcall);
        });
    });
}

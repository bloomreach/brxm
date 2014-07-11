/*
 * Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
    "use strict";

    angular.module('hippo.channel')

        .service('hippo.channel.FeedbackService', [
        '$log',
        '$filter',
        function ($log, $filter) {
            var feedbackService = {},
                translate = $filter('translate');

            // Assumption: translation id's look like SOME_KIND_OF_ERROR_CODE
            var translationIdRegex = /^\w+_/;
            var technicalErrorTranslationId = 'TECHNICAL_ERROR';

            feedbackService.getFeedback = function (errorResponse) {
                var translationId = errorResponse.errorCode;
                $log.info("Client exception: " + errorResponse.message);
                var interpolateParams = errorResponse.data;
                if (translationId && translationId.match(translationIdRegex)) {
                    var clientErrorMessage = translate(translationId, interpolateParams);
                    if (clientErrorMessage.match(translationIdRegex)) {
                        // Apparently there is no translation
                        return {message: translate(technicalErrorTranslationId)};
                    } else {
                        return {message: clientErrorMessage};
                    }
                } else {
                    return {message: translate(technicalErrorTranslationId)};
                }
            };

            return feedbackService;
        }
    ]);
}());

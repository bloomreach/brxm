/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

describe('Feedback Service', function () {
    'use strict';

    var feedbackService, translateMock, translatedMessage = 'message';

    beforeEach(function () {

        translateMock = jasmine.createSpy('translateSpy').andCallFake(function (id) {
            if (id === 'NO_TRANSLATION') {
                return id;
            } else {
                return translatedMessage;
            }
        });
        translateMock.storage = jasmine.createSpy('storage');
        translateMock.storageKey = jasmine.createSpy('storageKey');
        translateMock.preferredLanguage = jasmine.createSpy('preferredLanguage');

        module('hippo.channelManager.menuManager', function($provide) {
            $provide.value('$translate', translateMock);
        });

        inject(['hippo.channelManager.menuManager.FeedbackService', function(FeedbackService) {
            feedbackService = FeedbackService;
        }]);
    });

    it('should exist', function () {
        expect(feedbackService).toBeDefined();
    });

    it('should return server error feedback if message is absent', function () {

        var feedback = feedbackService.getFeedback({});
        expect(feedback.message).toBe(translatedMessage);
        expect(translateMock).toHaveBeenCalledWith('TECHNICAL_ERROR');
    });

    it('should return server error feedback if message is not a translationId', function () {

        var feedback = feedbackService.getFeedback({message: 'does not match with regex'});
        expect(feedback.message).toBe(translatedMessage);
        expect(translateMock).toHaveBeenCalledWith('TECHNICAL_ERROR');
    });

    it('should return server error feedback if translationId is absent', function () {

        var feedback = feedbackService.getFeedback({message: 'NO_TRANSLATION'});
        expect(feedback.message).toBe(translatedMessage);
        expect(translateMock).toHaveBeenCalledWith('TECHNICAL_ERROR');
    });

    it('should return client error feedback if translationId is present', function () {

        var feedback = feedbackService.getFeedback({message: 'THIS_IS_A_TRANSLATION_ID', data: {}});
        expect(feedback.message).toBe(translatedMessage);
        expect(translateMock).toHaveBeenCalledWith('THIS_IS_A_TRANSLATION_ID', {});
    });
});

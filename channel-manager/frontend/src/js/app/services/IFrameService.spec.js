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

describe('IFrame Service', function () {
    'use strict';

    var iframeConfig, parentIFramePanel, $log, $window, iframeService;

    beforeEach(module('hippo.channelManager.menuManagement'));

    beforeEach(function() {
        iframeConfig = {
            testProperty: 'testValue'
        };

        parentIFramePanel = {
            initialConfig: {
                iframeConfig: iframeConfig
            }
        };

        $window = {
            location: {
                search: '?parentExtIFramePanelId=ext-42'
            },
            self: $window,
            parent: {
                Ext: {
                    getCmp: function() {
                        return parentIFramePanel;
                    }
                }
            },
            document: jasmine.createSpyObj('document', ['getElementsByTagName', 'createElement'])
        };

        module(function($provide) {
            $provide.value('$window', $window);
        });
    });

    beforeEach(inject(['$log', 'hippo.channelManager.menuManagement.IFrameService', function (_$log_, $IFrameService) {
        $log = _$log_;
        iframeService = $IFrameService;
    }]));

    it('should exist', function() {
        expect(iframeService).toBeDefined();
    });

    it('should be active', function() {
        expect(iframeService.isActive).toEqual(true);
    });

    it('should return the parent IFramePanel as the container', function() {
        expect(iframeService.getContainer()).toEqual(parentIFramePanel);
    });

    it('should return the iframe config from the parent', function() {
        expect(iframeService.getConfig()).toEqual(iframeConfig);
    });

    it('should enable live reload in debug mode', function() {
        iframeConfig.debug = true;

        var head = jasmine.createSpyObj('head', ['appendChild']);
        $window.document.getElementsByTagName.andReturn([head]);

        spyOn($log, 'info');

        iframeService.enableLiveReload();

        expect(head.appendChild).toHaveBeenCalled();
        expect($log.info).toHaveBeenCalledWith('iframe #ext-42 has live reload enabled via //localhost:35729/livereload.js');
    });

    it('should not enable live reload in non-debug mode', function() {
        iframeConfig.debug = false;
        iframeService.enableLiveReload();
        expect($window.document.getElementsByTagName).not.toHaveBeenCalled();
    });

    it("should throw an error when the ID of the parent's IFramePanel is not known", function() {
        $window.location.search = '';
        expect(iframeService.getConfig).toThrow(new Error("Expected query parameter 'parentExtIFramePanelId'"));
    });

    it("should throw an error when the parent does not contain an IFramePanel with the given ID", function() {
        spyOn($window.parent.Ext, 'getCmp').andReturn(undefined);
        expect(iframeService.getConfig).toThrow(new Error("Unknown iframe panel id: 'ext-42'"));
    });

    it("should throw an error when the parent's IFramePanel does not contain any configuratin for the iframe", function() {
        parentIFramePanel.initialConfig.iframeConfig = undefined;
        expect(iframeService.getConfig).toThrow(new Error("Parent iframe panel does not contain iframe configuration"));
    });

});

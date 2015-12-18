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

describe('ChannelService', function () {
  'use strict';

  var channelService;
  var channelMock;
  var sessionServiceMock;

  beforeEach(function () {
    module('hippo-cm');

    channelMock = {
      contextPath: '/testContextPath',
      hostname: 'test.host.name'
    };

    sessionServiceMock = {
      authenticate: function(channel) {
        return new Promise(function(resolve, reject) {
          resolve(channel);
        });
      }
    };

    module(function ($provide) {
      $provide.value('SessionService', sessionServiceMock);
    });
  });

  beforeEach(inject([
    'ChannelService', function (ChannelService) {
      channelService = ChannelService;
    }
  ]));

  it('should load a channel and return promise', function(done) {
    channelService
      .load(channelMock)
      .then(function(channel) {
        expect(channel).toEqual(channelMock);
        expect(channelService.channel).toEqual(channelMock);
        done();
      });
  });

  it('should load a channel with an empty path if none is provided', function(done) {
    channelService
      .load(channelMock)
      .then(function() {
        expect(channelService.path).toEqual('');
        done();
      });
  });

  it('should load a channel with an optional path', function(done) {
    channelService
      .load(channelMock, 'optional/path')
      .then(function() {
        expect(channelService.path).toEqual('optional/path');
        done();
      });
  });


    it('should return a url that starts with the contextPath', function(done) {
      channelService
        .load({ contextPath: '/test' }, '/optional/path')
        .then(function() {
          expect(channelService.getUrl()).toEqual('/test/optional/path');
          done();
        });
    });

    it('should return a url that ends with a slash if it equals the contextPath', function(done) {
      channelService
        .load({ contextPath: '/test' })
        .then(function() {
          expect(channelService.getUrl()).toEqual('/test/');
          done();
        });
    });

    it('should return a url without the contextPath if it is root', function(done) {
      channelService
        .load({ contextPath: '/' })
        .then(function() {
          expect(channelService.getUrl()).toEqual('');
          done();
        });
    });

    it('should return a url with the cmsPreviewPrefix appended after the contextPath with a slash', function(done) {
      channelService
        .load({ contextPath: '/test', cmsPreviewPrefix: 'cmsPreviewPrefix' })
        .then(function() {
          expect(channelService.getUrl()).toEqual('/test/cmsPreviewPrefix');
          done();
        });
    });

  it('should return a url with the mountPath appended after the cmsPreviewPrefix', function(done) {
    channelService
      .load({ contextPath: '/test', cmsPreviewPrefix: 'cmsPreviewPrefix', mountPath: '/mountPath' })
      .then(function() {
        expect(channelService.getUrl()).toEqual('/test/cmsPreviewPrefix/mountPath');
        done();
      });
  });

});

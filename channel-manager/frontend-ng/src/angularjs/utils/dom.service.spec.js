/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

describe('DomService', function () {
  'use strict';

  var DomService;
  var $window;
  var fixturesPath = '/' + jasmine.getFixtures().fixturesPath;

  beforeEach(function () {
    module('hippo-cm.utils');

    module(function ($provide) {
      $provide.value('$document', {
        location: {
          pathname: '/app/root/index.html',
          host: 'localhost:8080',
        },
      });
    });

    inject(function (_DomService_, _$window_) {
      DomService = _DomService_;
      $window = _$window_;
    });

    jasmine.getFixtures().load('utils/dom.service.fixture.html');
  });

  function testInIframe(callback) {
    var iframe = $j('#testIframe');
    iframe.on('load', function () {
      callback(iframe[0].contentWindow);
    });
    iframe.attr('src', fixturesPath + '/utils/dom.service.iframe.fixture.html');
  }

  it('should add a css file to the head', function (done) {
    testInIframe(function (iframeWindow) {
      var cssFile = fixturesPath + '/utils/dom.service.fixture.css';
      DomService.addCss(iframeWindow, cssFile)
        .then(function () {
          var red = $j(iframeWindow.document).find('#red');
          expect(red.css('color')).toEqual('rgb(255, 0, 0)');
          done();
        });
    });
  });

  it('should add a script file to the body', function (done) {
    testInIframe(function (iframeWindow) {
      var script = fixturesPath + '/utils/dom.service.fixture.js';
      expect(iframeWindow.DomServiceTestScriptLoaded).not.toBeDefined();
      DomService.addScript(iframeWindow, script)
        .then(function () {
          expect(iframeWindow.DomServiceTestScriptLoaded).toEqual(true);
          done();
        });
    });
  });

  it('should reject a promise when script url returns 404', function (done) {
    testInIframe(function (iframeWindow) {
      DomService.addScript(iframeWindow, 'does-not-exist.js').catch(done);
    });
  });

  it('should return app root url with // protocol', function () {
    expect(DomService.getAppRootUrl()).toEqual('//localhost:8080/app/root/');
  });

});

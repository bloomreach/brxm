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

describe('DomService', () => {
  'use strict';

  let DomService;
  const fixturesPath = `/${jasmine.getFixtures().fixturesPath}`;

  beforeEach(() => {
    module('hippo-cm.utils');

    module(($provide) => {
      $provide.value('$document', [{
        location: {
          pathname: '/app/root/index.html',
          host: 'localhost:8080',
        },
      }]);
    });

    inject((_DomService_) => {
      DomService = _DomService_;
    });

    jasmine.getFixtures().load('utils/dom.service.fixture.html');
  });

  function testInIframe(callback) {
    const iframe = $j('#testIframe');
    iframe.on('load', () => {
      callback(iframe[0].contentWindow);
    });
    iframe.attr('src', `${fixturesPath}/utils/dom.service.iframe.fixture.html`);
  }

  it('should add a css file to the head', (done) => {
    testInIframe((iframeWindow) => {
      const cssFile = `${fixturesPath}/utils/dom.service.fixture.css`;
      DomService.addCss(iframeWindow, cssFile)
        .then(() => {
          const red = $j(iframeWindow.document).find('#red');
          expect(red.css('color')).toEqual('rgb(255, 0, 0)');
          done();
        });
    });
  });

  it('should add a script file to the body', (done) => {
    testInIframe((iframeWindow) => {
      const script = `${fixturesPath}/utils/dom.service.fixture.js`;
      expect(iframeWindow.DomServiceTestScriptLoaded).not.toBeDefined();
      DomService.addScript(iframeWindow, script)
        .then(() => {
          expect(iframeWindow.DomServiceTestScriptLoaded).toEqual(true);
          done();
        });
    });
  });

  it('should reject a promise when script url returns 404', (done) => {
    testInIframe((iframeWindow) => {
      DomService.addScript(iframeWindow, 'does-not-exist.js').catch(done);
    });
  });

  it('should return app root url with // protocol', () => {
    expect(DomService.getAppRootUrl()).toEqual('//localhost:8080/app/root/');
  });
});

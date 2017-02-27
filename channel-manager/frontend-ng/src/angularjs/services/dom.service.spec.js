/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('DomService', () => {
  let BrowserService;
  let DomService;
  const fixturesPath = `/${jasmine.getFixtures().fixturesPath}`;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    angular.mock.module(($provide) => {
      $provide.value('$document', [{
        location: {
          pathname: '/app/root/index.html',
          host: 'localhost:8080',
        },
      }]);
    });

    inject((_BrowserService_, _DomService_) => {
      BrowserService = _BrowserService_;
      DomService = _DomService_;
    });

    jasmine.getFixtures().load('services/dom.service.fixture.html');
  });

  function testInIframe(callback) {
    const iframe = $j('#testIframe');
    iframe.on('load', () => {
      callback(iframe[0].contentWindow);
    });
    iframe.attr('src', `${fixturesPath}/services/dom.service.iframe.fixture.html`);
  }

  it('can add a css file to the head', (done) => {
    testInIframe((iframeWindow) => {
      const cssFile = `${fixturesPath}/services/dom.service.fixture.css`;
      $j.get(cssFile).done((cssData) => {
        DomService.addCss(iframeWindow, cssData);
        const red = $j(iframeWindow.document).find('#red');
        expect(red.css('color')).toEqual('rgb(255, 0, 0)');
        done();
      }).fail(fail);
    });
  });

  it('can add a script file to the body', (done) => {
    testInIframe((iframeWindow) => {
      const script = `${fixturesPath}/services/dom.service.fixture.js`;
      expect(iframeWindow.DomServiceTestScriptLoaded).not.toBeDefined();
      DomService.addScript(iframeWindow, script)
        .then(() => {
          expect(iframeWindow.DomServiceTestScriptLoaded).toEqual(true);
          done();
        });
    });
  });

  it('rejects a promise when a script url returns 404', (done) => {
    testInIframe((iframeWindow) => {
      DomService.addScript(iframeWindow, 'does-not-exist.js').catch(done);
    });
  });

  it('returns the app root url with // protocol', () => {
    expect(DomService.getAppRootUrl()).toEqual('//localhost:8080/app/root/');
  });

  function expectEqualComputedStyle(elements1, elements2) {
    expect(elements1.length).toEqual(elements2.length);

    for (let i = 0; i < elements1.length; i += 1) {
      const computedStyle1 = window.getComputedStyle(elements1[i]);
      const computedStyle2 = window.getComputedStyle(elements2[i]);
      expect(computedStyle1.cssText).toEqual(computedStyle2.cssText);
    }
  }

  it('can copy the computed style of an element', () => {
    window.loadStyleFixtures('services/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');
    DomService.copyComputedStyleExcept(source[0], target[0]);
    expectEqualComputedStyle(source, target);
  });

  it('merges the existing style attribute of the target with the copied computed style of the source element', () => {
    window.loadStyleFixtures('services/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    expect(source.css('width')).not.toEqual('42px');
    target[0].style.width = '42px';

    DomService.copyComputedStyleExcept(source[0], target[0], ['width']);

    expect(target.css('color')).toEqual(source.css('color'));
    expect(target.css('width')).toEqual('42px');
  });

  it('can copy the computed style of an element except excluded properties', () => {
    window.loadStyleFixtures('services/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    // don't copy the 'position' property, so the target should use the default value 'static'
    expect(source.css('position')).toEqual('fixed');
    expect(target.css('position')).toEqual('static');
    DomService.copyComputedStyleExcept(source[0], target[0], ['position']);
    expect(target.css('position')).toEqual('static');
  });

  it('can copy the computed style of descendants', () => {
    window.loadStyleFixtures('services/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    DomService.copyComputedStyleOfDescendantsExcept(source[0], target[0]);
    expectEqualComputedStyle(source.find('*'), target.find('*'));
  });

  it('can copy the computed style of descendants except excluded properties', () => {
    window.loadStyleFixtures('services/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    // 'color' is excluded from copying so the default value (black) should be inherited by the target
    expect(source.find('ul').css('color')).toEqual('rgb(0, 0, 255)');
    DomService.copyComputedStyleOfDescendantsExcept(source[0], target[0], ['color']);
    expect(target.find('ul').css('color')).toEqual('rgb(0, 0, 0)');
  });

  it('can create a mousedown event', () => {
    const mouseDownEvent = DomService.createMouseDownEvent(window, 100, 200);
    expect(mouseDownEvent.type).toEqual('mousedown');
    expect(mouseDownEvent.bubbles).toEqual(true);
    expect(mouseDownEvent.clientX).toEqual(100);
    expect(mouseDownEvent.clientY).toEqual(200);
    expect(mouseDownEvent.view).toEqual(window);
  });

  it('can create a mousedown event in Edge', () => {
    spyOn(BrowserService, 'isEdge').and.returnValue(true);
    const mouseDownEvent = DomService.createMouseDownEvent(window, 100, 200);
    expect(mouseDownEvent.type).toEqual('pointerdown');
    expect(mouseDownEvent.bubbles).toEqual(true);
    expect(mouseDownEvent.clientX).toEqual(100);
    expect(mouseDownEvent.clientY).toEqual(200);
    expect(mouseDownEvent.view).toEqual(window);
  });

  it('can create a mousedown event in IE11', () => {
    spyOn(BrowserService, 'isIE').and.returnValue(true);
    const mouseDownEvent = DomService.createMouseDownEvent(window, 100, 200);
    expect(mouseDownEvent.type).toEqual('MSPointerDown');
    expect(mouseDownEvent.bubbles).toEqual(true);
    expect(mouseDownEvent.clientX).toEqual(100);
    expect(mouseDownEvent.clientY).toEqual(200);
    expect(mouseDownEvent.view).toEqual(window);
  });

  it('can calculate the scroll bar width', () => {
    const width = DomService.getScrollBarWidth();
    expect(width).toBeGreaterThan(-1);
  });

  it('can check if an element is hidden on the page', () => {
    $j('.shouldBeHidden').each((index, el) => {
      expect(DomService.isVisible($j(el))).toBe(false);
    });
  });

  it('can check if an element is visible on the page', () => {
    $j('.shouldBeVisible').each((index, el) => {
      expect(DomService.isVisible($j(el))).toBe(true);
    });
  });

  it('can check whether the body is visible', () => {
    expect(DomService.isVisible($j(document.body))).toBe(true);
  });

  it('escapes HTML characters in strings', () => {
    expect(DomService.escapeHtml('&<>"\'/')).toEqual('&amp;&lt;&gt;&quot;&#x27;&#x2F;');
    expect(DomService.escapeHtml('<script>alert("xss")</script>')).toEqual('&lt;script&gt;alert(&quot;xss&quot;)&lt;&#x2F;script&gt;');
  });
});


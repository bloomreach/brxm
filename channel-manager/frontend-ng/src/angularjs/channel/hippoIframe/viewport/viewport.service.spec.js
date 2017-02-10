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

describe('ViewportService', () => {
  let ViewportService;
  let canvas;
  let sheet;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    inject((_ViewportService_) => {
      ViewportService = _ViewportService_;
    });

    jasmine.getFixtures().load('channel/hippoIframe/viewport/viewport.service.fixture.html');

    canvas = $j('#canvas');
    sheet = $j('#sheet');
    ViewportService.init(sheet);
  });

  function expectUnconstrained() {
    expect(ViewportService.getWidth()).toBe(0);
    expect(sheet.width()).toBe(canvas.width());
  }

  it('should not constrain the viewport by default', () => {
    expectUnconstrained();
  });

  it('should not constrain the viewport when the width is set to zero', () => {
    ViewportService.setWidth(0);
    expectUnconstrained();
  });

  it('should constrain the maximum width to the viewport', () => {
    ViewportService.setWidth(720);

    expect(ViewportService.getWidth()).toBe(720);
    expect(sheet.width()).toBe(720);
  });
});

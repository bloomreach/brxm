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

describe('collapse controller', () => {
  let $rootScope;
  let $animate;
  let $q;
  let collapseCtrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($compile, _$rootScope_, _$animate_, _$q_) => {
      $rootScope = _$rootScope_;
      $animate = _$animate_;
      $q = _$q_;

      const element = $compile(`
        <div collapse>
          <div class="collapse-toggle"></div>
          <div class="collapse-element" style="height: 10px;">
            test
          </div>
        </div>
      `)($rootScope.$new());

      collapseCtrl = element.controller('collapse');
    });
  });

  it('sets initial collapse to false', () => {
    expect(collapseCtrl.isCollapsed).toBe(false);
  });

  it('calls collapse when field is open', () => {
    spyOn(collapseCtrl, 'collapse');
    collapseCtrl.isCollapsed = false;

    collapseCtrl.toggle();

    expect(collapseCtrl.collapse).toHaveBeenCalled();
    expect(collapseCtrl.isCollapsed).toBe(true);
  });

  it('calls open when field is collapsed', () => {
    spyOn(collapseCtrl, 'open');
    collapseCtrl.isCollapsed = true;

    collapseCtrl.toggle();

    expect(collapseCtrl.open).toHaveBeenCalled();
    expect(collapseCtrl.isCollapsed).toBe(false);
  });

  it('gets and sets current height of toggled element on collapse', () => {
    collapseCtrl.collapse();

    expect(collapseCtrl.toggledElementHeight).toBe(10);
    expect(collapseCtrl.toggledElement.attr('style')).toBe('height: 0px;');
  });

  it('uses $animate to register an animation on addition of collapse class', () => {
    spyOn($animate, 'addClass').and.returnValue($q.resolve());

    collapseCtrl.collapse();

    expect($animate.addClass).toHaveBeenCalledWith(
      collapseCtrl.toggledElement,
      'collapsed',
      {
        from: { height: collapseCtrl.toggledElementHeight },
        to: { height: 0 },
      },
    );
  });

  it('uses $animate to register an animation on removal of collapse class', () => {
    spyOn($animate, 'removeClass').and.returnValue($q.resolve());

    collapseCtrl.open();

    expect($animate.removeClass).toHaveBeenCalledWith(
      collapseCtrl.toggledElement,
      'collapsed',
      {
        from: { height: 0 },
        to: { height: collapseCtrl.toggledElementHeight },
      },
    );
  });

  it('adds css class to toggle element so it can use css transitions where needed', () => {
    spyOn($animate, 'addClass').and.returnValue($q.resolve());

    collapseCtrl.collapse();

    $rootScope.$apply();

    expect(collapseCtrl.toggleTrigger.hasClass('closed')).toBe(true);
  });

  it('removes inline height style after open animation has completed', () => {
    spyOn($animate, 'removeClass').and.returnValue($q.resolve());

    collapseCtrl.open();

    $rootScope.$apply();

    expect(collapseCtrl.toggledElement.attr('style')).toBe(undefined);
  });
});

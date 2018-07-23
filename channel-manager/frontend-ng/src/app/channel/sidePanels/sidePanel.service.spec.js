/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('SidePanelService', () => {
  let $mdSidenav;
  let $q;
  let $rootScope;
  let ChannelService;
  let CmsService;
  let mockSideNav;
  let sideNavElement;
  let sidePanelElement;
  let SidePanelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    sideNavElement = angular.element('<md-sidenav md-component-id="side-panel-id"></md-sidenav>');
    sidePanelElement = angular.element('<div></div>');
    sidePanelElement.append(sideNavElement);

    mockSideNav = jasmine.createSpyObj('sideNav', ['isOpen', 'toggle', 'open', 'close']);
    $mdSidenav = jasmine.createSpy('$mdSidenav').and.returnValue(mockSideNav);

    angular.mock.module(($provide) => {
      $provide.value('$mdSidenav', $mdSidenav);
    });

    inject((_$q_, _$rootScope_, _ChannelService_, _CmsService_, _SidePanelService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      SidePanelService = _SidePanelService_;
    });

    mockSideNav.open.and.returnValue($q.resolve());
    mockSideNav.close.and.returnValue($q.resolve());
  });

  describe('initialize', () => {
    it('looks up a sideNav instance by "md-component-id" attribute value', () => {
      SidePanelService.initialize('left', sidePanelElement, sideNavElement);

      expect($mdSidenav).toHaveBeenCalledWith('side-panel-id');
    });

    it('adds className "side-panel-closed" to the sidePanelElement if it is closed on init', () => {
      mockSideNav.isOpen.and.returnValue(false);
      SidePanelService.initialize('left', sidePanelElement, sideNavElement);

      expect(sidePanelElement.hasClass('side-panel-closed')).toBe(true);
      expect(sidePanelElement.hasClass('side-panel-open')).toBe(false);
    });

    it('adds className "side-panel-open" to the sidePanelElement if it is open on init', () => {
      mockSideNav.isOpen.and.returnValue(true);
      SidePanelService.initialize('left', sidePanelElement, sideNavElement);

      expect(sidePanelElement.hasClass('side-panel-closed')).toBe(false);
      expect(sidePanelElement.hasClass('side-panel-open')).toBe(true);
    });

    it('stores a new panel object with a reference to the sideNav instance and the sidePanelElement', () => {
      SidePanelService.initialize('left', sidePanelElement, sideNavElement);

      expect(SidePanelService.panels.left).not.toBeUndefined();
      expect(SidePanelService.panels.left.sideNav).toEqual(mockSideNav);
      expect(SidePanelService.panels.left.sidePanelElement).toEqual(sidePanelElement);
      expect(SidePanelService.panels.left.fullScreen).toBe(false);
    });
  });

  it('toggles a named side-panel', () => {
    SidePanelService.initialize('left', sidePanelElement, sideNavElement);
    mockSideNav.isOpen.and.returnValue(false);

    SidePanelService.toggle('left');

    expect(mockSideNav.open).toHaveBeenCalled();

    mockSideNav.toggle.calls.reset();
    mockSideNav.isOpen.and.returnValue(true);

    SidePanelService.toggle('left');
    $rootScope.$digest();

    expect(mockSideNav.close).toHaveBeenCalled();
  });

  it('forwards the is-open check to the mdSidenav service', () => {
    SidePanelService.initialize('left', sidePanelElement, sideNavElement);

    mockSideNav.isOpen.and.returnValue(true);
    expect(SidePanelService.isOpen('left')).toBe(true);

    mockSideNav.isOpen.and.returnValue(false);
    expect(SidePanelService.isOpen('left')).toBe(false);
  });

  it('the is-open check works when the left side panel has not been rendered yet', () => {
    mockSideNav.isOpen.and.throwError('left side panel cannot be found');
    expect(SidePanelService.isOpen('left')).toBeFalsy();
  });

  it('closes a side panel if it is open', (done) => {
    SidePanelService.initialize('left', sidePanelElement, sideNavElement);
    mockSideNav.isOpen.and.returnValue(true);

    SidePanelService.close('left').then(() => {
      expect(mockSideNav.close).toHaveBeenCalled();
      done();
    });
    $rootScope.$digest();
  });

  it('skips closing a side panel if it is was already closed', (done) => {
    SidePanelService.initialize('left', sidePanelElement, sideNavElement);
    mockSideNav.isOpen.and.returnValue(false);

    SidePanelService.close('left').then(() => {
      expect(mockSideNav.close).not.toHaveBeenCalled();
      done();
    });
    $rootScope.$digest();
  });

  it('forwards the open call to the mdSidenav service', () => {
    SidePanelService.initialize('left', sidePanelElement, sideNavElement);

    SidePanelService.open('left');
    expect(mockSideNav.open).toHaveBeenCalled();
  });

  it('ignores the open call when the a side panel has not been rendered yet', () => {
    expect(() => {
      SidePanelService.open('left');
    }).not.toThrow(jasmine.any(Error));
  });

  it('adds classNames "side-panel-open" and "side-panel-closed" to the sidePanelElement', () => {
    SidePanelService.initialize('left', sidePanelElement, sideNavElement);

    SidePanelService.open('left');

    expect(sidePanelElement.hasClass('side-panel-closed')).toBe(false);
    expect(sidePanelElement.hasClass('side-panel-open')).toBe(true);

    mockSideNav.isOpen.and.returnValue(true);

    SidePanelService.close('left');
    $rootScope.$digest();

    expect(sidePanelElement.hasClass('side-panel-closed')).toBe(true);
    expect(sidePanelElement.hasClass('side-panel-open')).toBe(false);
  });

  describe('side-panel full-screen behavior', () => {
    it('does not throw an error and returns falsy if side-panel has not been initialized yet', () => {
      try {
        expect(SidePanelService.isFullScreen('left')).toBeFalsy();
        SidePanelService.setFullScreen('left', true);
        expect(SidePanelService.isFullScreen('left')).toBeFalsy();
      } catch (e) {
        fail(e);
      }
    });

    it('stores the full-screen state of the side-panels', () => {
      SidePanelService.initialize('left', sidePanelElement, sideNavElement);

      SidePanelService.setFullScreen('left', true);
      expect(SidePanelService.isFullScreen('left')).toBe(true);

      SidePanelService.setFullScreen('left', false);
      expect(SidePanelService.isFullScreen('left')).toBe(false);
    });

    it('reports usage statistics when a side-panel goes fullscreen', () => {
      spyOn(CmsService, 'reportUsageStatistic');

      SidePanelService.initialize('left', sidePanelElement, sideNavElement);
      SidePanelService.setFullScreen('left', true);

      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsFullScreen', { side: 'left' });

      CmsService.reportUsageStatistic.calls.reset();
      SidePanelService.setFullScreen('left', false);

      expect(CmsService.reportUsageStatistic).not.toHaveBeenCalled();
    });

    it('hides the toolbar when going fullscreen', () => {
      spyOn(ChannelService, 'setToolbarDisplayed');

      SidePanelService.initialize('left', sidePanelElement, sideNavElement);
      SidePanelService.setFullScreen('left', true);

      expect(ChannelService.setToolbarDisplayed).toHaveBeenCalledWith(false);

      SidePanelService.setFullScreen('left', false);

      expect(ChannelService.setToolbarDisplayed).toHaveBeenCalledWith(true);
    });
  });
});

import { ChildPromisedApi } from '@bloomreach/navapp-communication';

import { ClientAppService } from '../client-app/services';

import { CommunicationsService } from './communications.service';
import { OverlayService } from './overlay.service';

describe('CommunicationsService', () => {
  function setup(): {
    communicationsService: CommunicationsService;
    overlayService: OverlayService;
    clientAppService: ClientAppService;
    api: ChildPromisedApi;
  } {
    const overlayService = jasmine.createSpyObj({
      enable: jasmine.createSpy(),
      disable: jasmine.createSpy(),
    });

    const api = jasmine.createSpyObj({
      navigate: Promise.resolve(),
    });

    const clientAppService = jasmine.createSpyObj('clientAppService', {
      getApp: {
        api,
      },
      activateApplication: jasmine.createSpy(),
    });

    const communicationsService = new CommunicationsService(
      clientAppService,
      overlayService,
    );

    return {
      communicationsService,
      overlayService,
      clientAppService,
      api,
    };
  }

  it('should navigate', () => {
    const { communicationsService, clientAppService, api } = setup();
    const url = 'url';
    const path = 'test/test';

    expect(communicationsService).toBeTruthy();

    communicationsService.navigate(url, path).then(() => {
      expect(api.navigate).toHaveBeenCalledWith({ path });
      expect(clientAppService.activateApplication).toHaveBeenCalledWith(url);
    });
  });

  describe('parentApiMethods', () => {
    it('should expose the parentApiMethods', () => {
      const { communicationsService } = setup();

      expect(communicationsService.parentApiMethods.showMask).toBeDefined();
      expect(communicationsService.parentApiMethods.hideMask).toBeDefined();
    });

    it('should enable overlay', () => {
      const { communicationsService, overlayService } = setup();

      communicationsService.parentApiMethods.showMask();
      expect(overlayService.enable).toHaveBeenCalled();

      communicationsService.parentApiMethods.hideMask();
      expect(overlayService.disable).toHaveBeenCalled();
    });
  });
});

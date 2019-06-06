import { OverlayService } from './overlay.service';

describe('OverlayService', () => {
  function setup(): {
    overlayService: OverlayService;
  } {
    const overlayService = new OverlayService();

    return {
      overlayService,
    };
  }

  it('should save state for overlay visibility', () => {
    const { overlayService } = setup();
    overlayService.visible$.subscribe(visible => {
      expect(visible).toBe(false);
    });
  });

  it('should set visibility', () => {
    const { overlayService } = setup();

    overlayService.enable();
    overlayService.visible$
      .subscribe(visible => {
        expect(visible).toBe(true);
      })
      .unsubscribe();

    overlayService.disable();
    overlayService.enable();
    overlayService.disable();
    overlayService.visible$
      .subscribe(visible => {
        expect(visible).toBe(false);
      })
      .unsubscribe();
  });
});

import { TestBed } from '@angular/core/testing';

import { OverlayService } from './overlay.service';

describe('OverlayService', () => {
  function setup(): {
    overlayService: OverlayService;
  } {
    TestBed.configureTestingModule({
      providers: [OverlayService],
    });

    return {
      overlayService: TestBed.get(OverlayService),
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

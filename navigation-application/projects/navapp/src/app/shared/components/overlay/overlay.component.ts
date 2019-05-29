import { Component } from '@angular/core';
import { Observable } from 'rxjs';

import { OverlayService } from '../../../services';

@Component({
  selector: 'brna-overlay',
  templateUrl: 'overlay.component.html',
  styleUrls: ['overlay.component.scss'],
})
export class OverlayComponent  {
  constructor(
    private overlay: OverlayService,
  ) {}

  get isOverlayVisible$(): Observable<boolean> {
    return this.overlay.visible$;
  }

  onClicked(e: MouseEvent): void {
    e.stopPropagation();
  }
}

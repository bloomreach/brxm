import { Injectable } from '@angular/core';

@Injectable()
export class SiteSelectionSidePanelService {
  private opened = false;

  get isOpened(): boolean {
    return this.opened;
  }

  toggle(): void {
    this.opened = !this.opened;
  }

  close(): void {
    this.opened = false;
  }
}

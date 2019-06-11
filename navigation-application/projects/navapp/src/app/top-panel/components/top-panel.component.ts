import { Component } from '@angular/core';

import { SiteSelectionSidePanelService } from '../services';

@Component({
  selector: 'brna-top-panel',
  templateUrl: 'top-panel.component.html',
  styleUrls: ['top-panel.component.scss'],
})
export class TopPanelComponent {
  constructor(private siteSelectionPanelService: SiteSelectionSidePanelService) {}

  get isSidePanelOpened(): boolean {
    return this.siteSelectionPanelService.isOpened;
  }

  onButtonClick(): void {
    this.siteSelectionPanelService.toggle();
  }
}

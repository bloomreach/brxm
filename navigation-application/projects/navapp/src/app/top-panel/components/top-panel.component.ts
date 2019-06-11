import { Component } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { NavConfigService } from '../../services';
import { SiteSelectionSidePanelService } from '../services';

@Component({
  selector: 'brna-top-panel',
  templateUrl: 'top-panel.component.html',
  styleUrls: ['top-panel.component.scss'],
})
export class TopPanelComponent {
  constructor(
    private navConfigService: NavConfigService,
    private siteSelectionPanelService: SiteSelectionSidePanelService,
  ) {}

  get isSidePanelOpened(): boolean {
    return this.siteSelectionPanelService.isOpened;
  }

  get selectedSite$(): Observable<string> {
    return this.navConfigService.selectedSite$.pipe(
      map(site => site ? site.name : ''),
    );
  }

  onSiteSelectorClicked(): void {
    this.siteSelectionPanelService.toggle();
  }
}

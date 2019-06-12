import { Component } from '@angular/core';
import { Observable } from 'rxjs';

import { Site } from '../../models/dto';
import { NavConfigService } from '../../services';
import { SiteSelectionSidePanelService } from '../services';

@Component({
  selector: 'brna-top-panel',
  templateUrl: 'top-panel.component.html',
  styleUrls: ['top-panel.component.scss'],
})
export class TopPanelComponent {
  private site: Site;

  constructor(
    private navConfigService: NavConfigService,
    private siteSelectionPanelService: SiteSelectionSidePanelService,
  ) {}

  get selectedSite(): Site {
    return this.site;
  }

  set selectedSite(site: Site) {
    this.site = site;
    this.siteSelectionPanelService.close();
  }

  get sites$(): Observable<Site[]> {
    return this.navConfigService.sites$;
  }

  get isSidePanelOpened(): boolean {
    return this.siteSelectionPanelService.isOpened;
  }

  onSiteSelectorClicked(): void {
    this.siteSelectionPanelService.toggle();
  }

  onBackdropClicked(): void {
    this.siteSelectionPanelService.close();
  }
}

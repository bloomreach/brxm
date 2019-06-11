import { animate, style, transition, trigger } from '@angular/animations';
import { NestedTreeControl } from '@angular/cdk/tree';
import { Component, HostBinding } from '@angular/core';
import { MatTreeNestedDataSource } from '@angular/material';

import { Site } from '../../../models';
import { NavConfigService } from '../../../services';
import { SiteSelectionSidePanelService } from '../../services';

@Component({
  selector: 'brna-site-selection-side-panel',
  templateUrl: 'site-selection-side-panel.component.html',
  styleUrls: ['site-selection-side-panel.component.scss'],
  animations: [
    trigger('slideInOut', [
      transition(':enter', [
        style({ transform: 'translateX(100%)' }),
        animate('300ms ease-in-out', style({ transform: 'translateX(0%)' })),
      ]),
      transition(':leave', [
        animate('300ms ease-in-out', style({ transform: 'translateX(100%)' })),
      ]),
    ]),
  ],
})
export class SiteSelectionSidePanelComponent {
  @HostBinding('@slideInOut')
  animate = true;

  treeControl = new NestedTreeControl<Site>(node => node.subGroups);
  dataSource = new MatTreeNestedDataSource<Site>();

  constructor(
    private navConfigService: NavConfigService,
    private siteSelectionPanelService: SiteSelectionSidePanelService,
  ) {
    navConfigService.sites$.subscribe(sites => this.dataSource.data = sites);
  }

  hasChild(index: number, node: Site): boolean {
    return node.subGroups && node.subGroups.length > 0;
  }

  onLeafNodeClicked(site: Site): void {
    this.navConfigService.setSelectedSite(site);
    this.siteSelectionPanelService.close();
  }
}

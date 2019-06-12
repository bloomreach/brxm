import { animate, style, transition, trigger } from '@angular/animations';
import { NestedTreeControl } from '@angular/cdk/tree';
import { Component, EventEmitter, HostBinding, Input, OnChanges, Output } from '@angular/core';
import { MatTreeNestedDataSource } from '@angular/material';

import { Site } from '../../../models';

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
export class SiteSelectionSidePanelComponent implements OnChanges {
  @Input()
  sites: Site[];

  @Input()
  selectedSite: Site;

  @Output()
  selectedSiteChange = new EventEmitter<Site>();

  @HostBinding('@slideInOut')
  animate = true;

  searchText = '';
  treeControl = new NestedTreeControl<Site>(node => node.subGroups);
  dataSource = new MatTreeNestedDataSource<Site>();

  ngOnChanges(changes): void {
    this.dataSource.data = this.applyFilter(this.sites, this.searchText);
  }

  hasChild(index: number, node: Site): boolean {
    return node.subGroups && node.subGroups.length > 0;
  }

  onLeafNodeClicked(site: Site): void {
    this.selectedSiteChange.emit(site);
  }

  onSearchInputKeyUp(): void {
    this.dataSource.data = this.applyFilter(this.sites, this.searchText);
  }

  private applyFilter(sites: Site[], searchText: string): Site[] {
    if (!sites || !sites.length) {
      return [];
    }

    searchText = searchText.toLowerCase();

    return sites.reduce((result, site) => {
      site = { ...site };

      if (site.name.toLowerCase().includes(searchText)) {
        result.push(site);
        return result;
      }

      site.subGroups = this.applyFilter(site.subGroups, searchText);

      if (site.subGroups.length) {
        result.push(site);
      }

      return result;
    }, []);
  }
}

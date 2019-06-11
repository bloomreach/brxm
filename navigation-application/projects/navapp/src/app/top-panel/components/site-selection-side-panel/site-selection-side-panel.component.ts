import { animate, style, transition, trigger } from '@angular/animations';
import { FlatTreeControl } from '@angular/cdk/tree';
import { Component, HostBinding } from '@angular/core';
import { MatTreeFlatDataSource, MatTreeFlattener } from '@angular/material';

import { Site } from '../../../models';
import { NavConfigService } from '../../../services';

/** Flat node with expandable and level information */
interface ExampleFlatNode {
  expandable: boolean;
  name: string;
  level: number;
}

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

  treeControl = new FlatTreeControl<ExampleFlatNode>(
    node => node.level, node => node.expandable);

  treeFlattener = new MatTreeFlattener(
    this.transformer, node => node.level, node => node.expandable, node => node.subGroups);

  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  constructor(private navConfigService: NavConfigService) {
    navConfigService.sites$.subscribe(sites => this.dataSource.data = sites);
  }

  hasChild(index: number, node: ExampleFlatNode): boolean {
    return node.expandable;
  }

  onLeafNodeClicked(node: ExampleFlatNode): void {
    console.log('onLeafNodeClicked', node);
  }

  private transformer(node: Site, level: number): any {
    return {
      expandable: !!node.subGroups && node.subGroups.length > 0,
      name: node.name,
      level,
    };
  }
}

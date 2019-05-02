/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

export class MenuItemLink {
  iframe: HTMLIFrameElement;
  appPath: string;

  constructor(
    public id: string,
    public caption: string,
  ) {}
}

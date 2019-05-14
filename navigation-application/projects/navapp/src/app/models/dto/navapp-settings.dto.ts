interface NavAppSettings {
  userSettings: {
    userName: string;
    language: string;
    timeZone: string;
  };

  appSettings: {
    navConfigResources: NavConfigResource[];
  };
}

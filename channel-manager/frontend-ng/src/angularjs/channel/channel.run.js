export function run($state, CmsService, ChannelService) {
  'ngInject';

  function showChannel(channel) {
    $state.go('hippo-cm.channel', { channelId: channel.id }, { reload: true });
  }

  CmsService.subscribe('load-channel', (channel) => {
    ChannelService.load(channel).then(showChannel); // TODO: handle error.
  });

  // Handle reloading of iframe by BrowserSync during development
  CmsService.publish('reload-channel');
}

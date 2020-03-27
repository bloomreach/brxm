# brXM SPA Example Project
This Docker image provides an example backend for a JavaScript-based Single Page Application.

## What is Bloomreach Experience Manager?
[Bloomreach Experience Manager](https://www.bloomreach.com/en/products/experience-manager) (brXM) is an open and flexible CMS designed for developers and marketers. As the original headless CMS, brXM allows developers to build quickly and integrate with the systems. While it’s built for speed, it also provides top-notch personalization and channel management capabilities for marketers to drive results.

## Run
### Linux and Windows Hosts
```bash
docker run --net=host bloomreach/xm-spa-example
```

### Docker for Mac
```bash
docker run -p 8080:8080 bloomreach/xm-spa-example
```

Due to [this](https://github.com/docker/for-mac/issues/68) bug in Docker for Mac, the CMS instance cannot access the SPA port on the host machine.
To get it working, we should update the UrlRewriter [rule](https://documentation.bloomreach.com/library/concepts/spa-integration/url-rewriter-rules.html) to access [`host.docker.internal`](https://docs.docker.com/docker-for-mac/networking/) instead of `localhost`:
```xml
<rule>
  <!-- Rule 3: Proxy requests to route to the SPA server -->
  <from>^(?:/[^\/]+)?(?:/_cmsinternal)?([\?/].*)?$</from>
  <to type="proxy" last="true">http://host.docker.internal:3000$0</to>
</rule>
```

## Links
- [React-based application](https://code.onehippo.org/cms-community/bloomreach-spa-sdk/tree/bloomreach-spa-sdk-14.2.0/examples/react-csr);
- [Next.js-based application](https://code.onehippo.org/cms-community/bloomreach-spa-sdk/tree/bloomreach-spa-sdk-14.2.0/examples/react-ssr).

## License
Published under [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

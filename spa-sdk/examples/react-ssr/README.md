# brXM + Next.js = ♥️

Example Next.js SPA using the Bloomreach Experience [React SDK](https://www.npmjs.com/package/@bloomreach/react-sdk).
The app uses unversal framework [Next.js](https://github.com/zeit/next.js) for creating isomorphic React applications.

## Install and run
Run [Docker](https://hub.docker.com/r/bloomreach/xm-spa-example) container with the configured brXM instance:
```bash
docker run -p 8080:8080 bloomreach/xm-spa-example
```

Then, copy `.env.dist` file to `.env` and specify the brXM instance to fetch the page model from:
```
BRXM_ENDPOINT=http://localhost:8080/site/resourceapi
```

Finally, build and run the Next.js app as followed:

```bash
yarn
yarn dev
```

The CMS should now be accessible at <http://localhost:8080/cms>, and it should render the Next.js app in preview mode in the Experience manager.
The SPA itself can be accessed directly via <http://localhost:3000>.

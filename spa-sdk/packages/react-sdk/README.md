# Bloomreach React SDK
[![NPM](https://img.shields.io/npm/v/@bloomreach/react-sdk.svg)](https://www.npmjs.com/package/@bloomreach/react-sdk)
[![License](https://img.shields.io/npm/l/@bloomreach/react-sdk.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Bloomreach React SDK provides simplified headless integration with [Bloomreach Experience Manager](https://www.bloomreach.com/en/products/experience-manager) for React-based applications.
This library interacts with the [Page Model API](https://documentation.bloomreach.com/library/concepts/page-model-api/introduction.html) and [Bloomreach SPA SDK](https://www.npmjs.com/package/@bloomreach/spa-sdk) and exposes a simplified declarative React interface over the Page Model.

## What is Bloomreach Experience Manager?
Bloomreach Experience Manager (brXM) is an open and flexible CMS designed for developers and marketers. As the original headless CMS, brXM allows developers to build quickly and integrate with the systems. While it’s built for speed, it also provides top-notch personalization and channel management capabilities for marketers to drive results.

## Features
- brXM Page React component;
- brXM Components React component;
- Manage Content Button;
- Manage Menu Button;
- [Context API](https://reactjs.org/docs/context.html) support;
- [Hooks API](https://reactjs.org/docs/hooks-intro.html) support;
- [Next.js](https://nextjs.org/) support;
- [React Router](https://reacttraining.com/react-router/) and [Next Routes](https://github.com/fridays/next-routes) support;
- [Enzyme](https://airbnb.io/enzyme/) and [Jest](https://jestjs.io/) support.

## Get Started
### Installation
To get the SDK into your project with [NPM](https://docs.npmjs.com/cli/npm):
```bash
npm install @bloomreach/react-sdk
```

And with [Yarn](https://yarnpkg.com):
```bash
yarn add @bloomreach/react-sdk
```

### Usage
The following code snippet renders a simple page with a [Banner](https://documentation.bloomreach.com/library/setup/hst-components/overview.html) component.

```jsx
import React from 'react';
import axios from 'axios';
import { BrComponent, BrPage, BrPageContext, BrProps } from '@bloomreach/react-sdk';

function Banner({ component }: BrProps) {
  return <div>Banner: {component.getName()}</div>;
}

export default function App() {
  const config = { /* ... */ };

  return (
    <BrPage configuration={config} mapping={{ Banner }}>
      <header>
        <BrPageContext.Consumer>
          { page => <Link to={page.getUrl('/')} />Home</Link> }
        </BrPageContext.Consumer>
        <BrComponent path="menu"><Menu /></BrComponent>
      </header>
      <section>
        <BrComponent path="main" />
      </section>
      <BrComponent path="footer">
        <footer><BrComponent /></footer>
      </BrComponent>
    </BrPage>
  );
}
```

### Configuration
The `BrPage` component supports several options you may use to customize page initialization.
These options will be passed to the `initialize` function from [`@bloomreach/spa-sdk`](https://www.npmjs.com/package/@bloomreach/spa-sdk).
See [here](https://www.npmjs.com/package/@bloomreach/spa-sdk#configuration) for the full configuration documentation.

### Mapping
The `BrPage` component provides a way to link React components with the brXM ones.
It requires to pass the `mapping` property that maps the component type with its representation.
- The [Container Items](https://www.npmjs.com/package/@bloomreach/spa-sdk#container-item) can be mapped by their labels.
  ```jsx
  import NewsList from './components/NewsList';

  return <BrPage mapping={{ 'News List': NewsList }} />;
  ````
- The [Containers](https://www.npmjs.com/package/@bloomreach/spa-sdk#container-item) can be only mapped by their [type](https://documentation.bloomreach.com/library/concepts/template-composer/channel-editor-containers.html), so you need to use [constants](https://www.npmjs.com/package/@bloomreach/spa-sdk#constants) from [`@bloomreach/spa-sdk`](www.npmjs.com/package/@bloomreach/spa-sdk). By default, the React SDK provides an implementation for all the container types as it's defined in the [documentation](https://documentation.bloomreach.com/library/concepts/template-composer/channel-editor-containers.html).
  ```jsx
  import { TYPE_CONTAINER_INLINE } from '@bloomreach/spa-sdk';
  import MyInlineContainer from './components/MyInlineContainer';

  return <BrPage mapping={{ [TYPE_CONTAINER_INLINE]: MyInlineContainer }} />;
  ```

  From within the Container component, the Container Items can be accessed via the `children` property.
  This can be used to reorder or wrap child elements.
  ```jsx
  export default function MyInlineContainer() {
    return (
      <div>
        {React.Children.map(props.children, child => (
          <span className="float-left">
            {child}
          </span>
        ))}
      </div>
    );
  }
  ```

- The [Components](https://www.npmjs.com/package/@bloomreach/spa-sdk#component) can be mapped by their names. It is useful for a menu component mapping.
  ```jsx
  import Menu from './components/Menu';

  return <BrPage mapping={{ menu: Menu }} />;
  ```

### Inline Mapping
There is also another way to render a component.
In case you need to show a static component or a component from the abstract page, you can use inline component mapping.
```jsx
return <BrComponent path="menu"><Menu /></BrComponent>
```

It is also possible to point where the component's children are going to be placed.
```jsx
return (
  <BrComponent path="footer">
    <footer><BrComponent /></footer>
  </BrComponent>
);
```

The component data in case of inline mapping can be accessed via the `BrComponentContext`.
```jsx
return (
  <BrComponentContext.Consumer>
    {component => (
      <BrComponent path="footer">
        <footer>
          &copy; {component.getName()}
          <BrComponent />
        </footer>
      </BrComponent>
    )}
  </BrComponentContext.Consumer>
);
```

Or by using React Hooks.
```jsx
import { BrComponentContext } from '@bloomreach/react-sdk';

export default function Menu() {
  const component = React.useContext(BrComponentContext);

  return <ul>{component.getName()}</ul>;
}
```

### Reference
The React SDK is using [Bloomreach SPA SDK](https://www.npmjs.com/package/@bloomreach/spa-sdk#reference) to interact with the brXM.
The complete reference of the exposed JavaScript objects can be found [here](https://javadoc.onehippo.org/14.3/bloomreach-spa-sdk/).

#### BrPage
This is the entry point to the page model.
This component requests and initializes the page model, and then renders the page root component with React children passed to this component.
The component also sets the page object into `BrPageContext`.

Property | Required | Description
--- | :---: | ---
`configuration` | _yes_ | The [configuration](#configuration) of the SPA SDK.
`mapping` | _yes_ | The brXM and React components [mapping](#mapping).
`page` | _no_ | Preinitialized page instance or prefetched page model. Mostly that should be used to transfer state from the server-side to the client-side.

#### BrComponent
This component points to where children or some component should be placed. `BrComponent` can be used inside `BrPage` or mapped components only. If React children are passed, then they will be rendered [as-are](#inline-mapping). Otherwise, it will try to render all children components recursively.

Property | Required | Description
--- | :---: | ---
`path` | _no_ | The path to a component. The path is defined as a slash-separated components name chain relative to the current component (e.g. `main/container`). If it is omitted, all the children will be rendered.

#### BrManageContentButton
This component places a button on the page that opens the linked content in the document editor.
The button will only be shown in preview mode.

Property | Required | Description
--- | :---: | ---
`content` | _yes_ | The content entity to open for editing.

#### BrManageMenuButton
This component places a button on the page that opens the linked menu in the menu editor.
The button will only be shown in preview mode.

Property | Required | Description
--- | :---: | ---
`menu` | _yes_ | The related menu model.

#### BrComponentContext
The [React Context](https://reactjs.org/docs/context.html) holding the current brXM [Component](https://www.npmjs.com/package/@bloomreach/spa-sdk#component).

#### BrPageContext
The [React Context](https://reactjs.org/docs/context.html) holding the current brXM [Page](https://www.npmjs.com/package/@bloomreach/spa-sdk#page).

## Links
- [SPA integration concept](https://documentation.bloomreach.com/library/concepts/spa-integration/introduction.html).
- [Page Model API introduction](https://documentation.bloomreach.com/library/concepts/page-model-api/introduction.html).
- [Bloomreach SPA SDK](https://www.npmjs.com/package/@bloomreach/spa-sdk).

## FAQ
- Information about common problems and possible solutions can be found on [the troubleshooting page](https://documentation.bloomreach.com/library/concepts/spa-integration/troubleshooting.html).
- Information about the recommended setup can be found on [the best practices page](https://documentation.bloomreach.com/library/concepts/spa-integration/best-practices.html).

## License
Published under [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

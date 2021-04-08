const { ApolloServer, defaultPlaygroundOptions } = require("apollo-server-express");
const { ApolloGateway, RemoteGraphQLDataSource } = require("@apollo/gateway");
const { configureKeycloak } = require('./lib/keycloak')
const { KeycloakContext } = require('keycloak-connect-graphql');
const expressPlayground = require('graphql-playground-middleware-express').default;
const express = require('express');

const app = express();
const port = 4000;
const graphqlPath = '/graphql';
const playgroundPath = '/playground';
const gateway = new ApolloGateway({
  serviceList: [
    { name: "adapter", url: "http://localhost:4001" },
    { name: "bogos", url: "http://localhost:4002" }
  ],
  buildService({ name, url }) {
    return new RemoteGraphQLDataSource({
      url,
      willSendRequest({ request, context }) {
        console.log(`${__filename}::willSendRequest(${name})`, request, context);
        // Passing Keycloak Access Token to services
        if (context.kauth && context.kauth.accessToken) {
          request.http.headers.set('Authorization', 'bearer ' + context.kauth.accessToken.token);
        }
      }
    })
  },
  // Experimental: Enabling this enables the query plan view in Playground.
  __exposeQueryPlanExperimental: true,
});

(async () => {
  // perform the standard keycloak-connect middleware setup on our app
  const { keycloak } = configureKeycloak(app, graphqlPath);

  // Ensure entire GraphQL Api can only be accessed by authenticated users
  app.use(playgroundPath, keycloak.protect());

  const server = new ApolloServer({
    gateway,

    // Apollo Graph Manager (previously known as Apollo Engine)
    // When enabled and an `ENGINE_API_KEY` is set in the environment,
    // provides metrics, schema management and trace reporting.
    engine: false,

    // Subscriptions are unsupported but planned for a future Gateway version.
    subscriptions: false,

    // Disable default playground
    playground: false,

    context: ({ req }) => {
      console.log(`ApolloServer::${req}`)
      const kauth = new KeycloakContext({ req });
      return { kauth };
    }
  });
  await server.start();

  // Handle custom GraphQL Playground to use dynamics header token from keycloak
  app.get(playgroundPath, (req, res, next) => {
    const headers = JSON.stringify({
      'X-CSRF-Token': req.kauth.grant.access_token.token,
    });
    expressPlayground({
      ...defaultPlaygroundOptions,
      endpoint: `${graphqlPath}?headers=${encodeURIComponent(headers)}`,
      settings: {
        ...defaultPlaygroundOptions.settings,
        'request.credentials': 'same-origin',
      },
      version: "",
      tabs: ""
    })(req, res, next);
  });

  server.applyMiddleware({ app });

  app.listen({ port }, () => {
    console.log(`🚀 Server ready at http://localhost:${port}${server.graphqlPath}`);
  });

})();

const { ApolloServer, gql } = require('apollo-server');
const { buildFederatedSchema } = require("@apollo/federation");
const port = 4002;

const typeDefs = gql`
  type Authority {
      id: ID!
      name: String
      code: String
  }

  type Query {
    authority: Authority
  }
`;

const db = {
    authority: { id: 1, name: "IDF Mobilité", code: "idfm" }
}

const resolvers = {
    Query: {
        authority: () => db.authority,
    },
};

// The ApolloServer constructor requires two parameters: your schema
// definition and your set of resolvers.
const server = new ApolloServer(
    //{ typeDefs, resolvers }
    { schema: buildFederatedSchema([{ typeDefs, resolvers }]) }
);

// The `listen` method launches a web server.
server.listen({ port }).then(({ url }) => {
    console.log(`🚀  Server ready at ${url}`);
});
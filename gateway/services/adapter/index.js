const { ApolloServer, gql } = require("apollo-server");
const { buildFederatedSchema } = require("@apollo/federation");
const fetch = require("node-fetch");

const port = 4001;
const apiUrl = "http://localhost:4005";

const typeDefs = gql`
  enum Permission {
      AGENT_CREATE,
      AGENT_UPDATE,
      AGENT_READ
  }
  type Agent @key(fields: "id") {
    id: ID!
    name: String
    permissions: [Permission]
  }
  type SamType {
      id: ID!
      name: String
  }
  extend type Query {
    agent(id: ID!): Agent
    agents: [Agent]
    samTypes: [SamType]
  }
`;

const resolvers = {
  Agent: {
    __resolveReference(ref) {
      return fetch(`${apiUrl}/agents/${ref.id}`).then(res => res.json());
    }
  },
  Query: {
    agent(_, { id }) {
      return fetch(`${apiUrl}/agents/${id}`).then(res => res.json());
    },
    agents() {
      return fetch(`${apiUrl}/agents`).then(res => res.json());
    },
    samTypes() {
      return fetch(`${apiUrl}/samtypes`).then(res => res.json());
    }
  }
};

const server = new ApolloServer({
  schema: buildFederatedSchema([{ typeDefs, resolvers }])
});

server.listen({ port }).then(({ url }) => {
  console.log(`Adapter service ready at ${url}`);
});

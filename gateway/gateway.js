const { ApolloServer } = require("apollo-server");
const { ApolloGateway } = require("@apollo/gateway");

const port = 4000;

const gateway = new ApolloGateway({
  serviceList: [
    { name: "adapter", url: "http://localhost:4001" },
    { name: "bogos", url: "http://localhost:4002" }
  ]
});

const server = new ApolloServer({
  gateway,
  subscriptions: false
});

server.listen({ port }).then(({ url }) => {
  console.log(`Server ready at ${url}`);
});
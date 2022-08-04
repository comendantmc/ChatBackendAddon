# ChatBackendAddon

This plugin allows you to preprocess [PistonChat](https://github.com/AlexProgrammerDE/PistonChat) messages on an external backend server.

## Config

If you don't have DNS load balancer you could add the addresses of the replicas manually or just have one server to handle all requests. Retries field specifies how many times the plugin will try to process one message if it fails.

```yaml
enable: true
allow-on-error: true
timeout: 1000
retries: 1
enable-metrics: true
servers: # load balanced with round-robin
  - http://localhost:25501
```

## Backend server example (Node)

You can write it in any language as long as the server accepts POST requests and returns a JSON object which contains at least an "allow" boolean field.

```js
const fastify = require('fastify')({ logger: true });

fastify.post('/', async (request, reply) => {
  const { username, message } = request.body;
  console.log({ username, message });
  return {
    allowed: !message.match(/1\s*\+\s*1/g),
    replacement: message.match(/2/g) ? message.replace(/2/g, '3') : null
  };
});

const start = async () => {
  try {
    await fastify.listen(25501);
  } catch (err) {
    fastify.log.error(err);
    process.exit(1);
  }
}
start();
```

## Building from source

```bash
git clone https://github.com/comendantmc/ChatBackendAddon.git
cd ChatBackendAddon
mvn package
```

## Stats

[![bstats](https://bstats.org/signatures/bukkit/ChatBackendAddon.svg)](https://bstats.org/plugin/bukkit/ChatBackendAddon/16028)
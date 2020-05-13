*Build:* `mvn package`

*Run consensus:* copy produced jar, `lib` folder, and example config file (renamed `consensus.properties`) to destination server. Make sure you set `id` appropriately. For local testing, use separate directories and change the `ipcPort`. Run `java -jar consensus-1.0-SNAPSHOT.jar` to start the server.

*Run crypto vote protocol:* `java -cp consensus-1.0-SNAPSHOT.jar consensus.CryptoClient`

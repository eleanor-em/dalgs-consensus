**Build:** `mvn package`

**Run consensus:** copy produced jar, `lib` folder, and example config file (renamed `consensus.properties`) to destination server. Make sure you set `id` appropriately. For local testing, use separate directories and change the `ipcPort`. Run `java -jar consensus-beta.jar` to start the server.

**Run crypto vote protocol:** `java -cp consensus-beta.jar consensus.CryptoClient`

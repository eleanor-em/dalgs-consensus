**Install dependencies & build:**

`./install.sh` (tested on Ubuntu 18.04 and 20.04)

**Run consensus:**

Copy produced jar, `lib` folder, and example config file (renamed to `consensus.properties` rather than `consensus.properties.example`) to destination servers.

Config options:
* `hosts`: a comma-separated list of host addresses that will run a consensus node, for this node to connect to. Should also include this node's address
* `retryTime`: a number of milliseconds to wait if the node fails to connect to another node before retrying the connection
* `mode`: either `debug` or `release`. The debug mode will run all of the nodes locally, whereas the release mode will only run one node (that will connect to foreign nodes).
* `ipcPort`: the port to listen for clients who will interact with the consensus network.
* `ipcServer`: the address that the client will connect to. Should typically be `localhost:<ipcPort>`.
* `id`: the ID number of this node, starting from 0. When used as an index into `hosts`, the address should be this node's address.
* `p`: the prime number to use for ElGamal encryption. Should be of the form `p = 2q + 1` for another prime `q`.
* `leaderLag`: the number of milliseconds a Raft leader should delay in its update step. This can be used to simulate an unreliable connection in the `debug` mode.
* `raftDelay`: controls the Raft election timeouts -- timeouts are randomly generated between `raftDelay` and `2 * raftDelay`.
* `initialBalance`: the initial balance of blockchain wallets used for proof-of-work
* `initialPowDifficulty`: the initial difficulty parameter for proof-of-work
* `mineRate`: the rate at which a proof-of-work node will mine

To start a node, run `java -jar consensus-1.0.jar`.

**Run crypto vote protocol:**

`java -cp consensus-1.0.jar consensus.CryptoDriver <session name>`

Each run of the crypto voting should be done with a different session name; this allows participants to recover from crashing by reloading the private key share generated for that particular session from a local database. Session names can be any string.

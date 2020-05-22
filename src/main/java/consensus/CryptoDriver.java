package consensus;

import com.google.gson.JsonParseException;
import consensus.crypto.LocalKeygenShare;
import consensus.crypto.PostVoteMessage;
import consensus.ipc.IpcClient;
import consensus.net.data.HostPort;
import consensus.util.ConfigManager;
import consensus.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class CryptoDriver {
    private static final Logger log = LogManager.getLogger(CryptoDriver.class);
    private static Connection dbConn = null;

    public static void saveSessionData(CryptoSessionData data) {
        try {
            var updateStatement = dbConn.prepareStatement("REPLACE INTO SessionData (sessionId, keygenShare, voteMsg) VALUES (?, ?, ?)");
            updateStatement.setString(1, data.sessionId);
            updateStatement.setString(2, data.keyShare.map(StringUtils::toJson).orElse(""));
            updateStatement.setString(3, data.voteMsg.map(StringUtils::toJson).orElse(""));
            updateStatement.executeUpdate();
        } catch (SQLException ignored) {
            ignored.printStackTrace();
            log.warn("failed to save key share");
        }
    }

    private static CryptoSessionData loadDatabase(String sessionId) {
        CryptoSessionData sessionData = new CryptoSessionData(sessionId);

        try {
            Class.forName("org.sqlite.JDBC");

            try {
                dbConn = DriverManager.getConnection("jdbc:sqlite:vote.db");

                var createTableStatement = dbConn.createStatement();
                createTableStatement.setQueryTimeout(5);
                createTableStatement.executeUpdate("CREATE TABLE IF NOT EXISTS SessionData (id INTEGER PRIMARY KEY, sessionId TEXT, keygenShare TEXT, voteMsg TEXT);");
                createTableStatement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx ON SessionData (sessionId)");

                var fetchStatement = dbConn.prepareStatement("SELECT * FROM SessionData WHERE sessionId = ?;");
                fetchStatement.setString(1, sessionId);
                ResultSet results = fetchStatement.executeQuery();

                while (results.next()) {
                    try {
                        var keyShare = (LocalKeygenShare) StringUtils.fromJson(results.getString("keygenShare"), LocalKeygenShare.class);
                        sessionData.keyShare = Optional.ofNullable(keyShare);
                    } catch (JsonParseException ignored) {
                        log.warn("error parsing keygenShare");
                    }

                    try {
                        var keyShare = (PostVoteMessage) StringUtils.fromJson(results.getString("voteMsg"), PostVoteMessage.class);
                        sessionData.voteMsg = Optional.ofNullable(keyShare);
                    } catch (JsonParseException ignored) {
                        log.warn("error parsing voteMsg");
                    }
                }

            } catch (SQLException e) {
                log.warn("error executing SQL:");
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            log.fatal("could not load SQLite");
            System.exit(1);
        }

        if (dbConn == null) {
            log.fatal("failed to open SQLite connection");
            System.exit(1);
        }

        return sessionData;
    }

    public static void main(String[] args) {
        // Load session name from command line
        String sessionId = "default";
        if (args.length > 0) {
            sessionId = args[0];
        }
        log.info("using session \"" + sessionId + "\"");

        ConfigManager.loadProperties();
        var ipcServerString = ConfigManager.getString("ipcServer");
        if (ipcServerString.isEmpty()) {
            System.exit(2);
        }

        var ipcServer = HostPort.tryFrom(ipcServerString.get());
        if (ipcServer.isEmpty()) {
            log.fatal("could not interpret address: " + ipcServerString.get());
            System.exit(2);
        }

        var peerCount = ConfigManager.getString("hosts").orElse("").split(",").length;
        if (peerCount == 0) {
            log.fatal("Must have at least one peer. Check \"hosts\" in the configuration file.");
            System.exit(2);
        }

        var sessionData = loadDatabase(sessionId);
        var cryptoClient = new CryptoClient(sessionData, peerCount);
        IpcClient.open(ipcServer.get(), cryptoClient);
        cryptoClient.run();
        System.exit(0);
    }
}

package consensus.ipc;

import consensus.IConsensusClient;
import consensus.net.IoSocket;
import consensus.net.data.HostPort;
import consensus.net.data.IncomingMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class IpcClient {
    private static final Logger log = LogManager.getLogger(IpcClient.class);
    private final IoSocket socket;
    private final IConsensusClient receiver;

    private IpcClient(IoSocket socket, IConsensusClient receiver) {
        this.socket = socket;
        this.receiver = receiver;
    }

    private void readTask() {
        while (socket.isOpen()) {
            var line = socket.readLine();
            if (line.isPresent()) {
                var incoming = IncomingMessage.tryFrom(line.get());
                incoming.ifPresent(receiver::receiveEntry);
            }
            Thread.yield();
        }
        log.error("disconnected from IPC server");
        System.exit(-1);
    }

    private void writeTask() {
        while (socket.isOpen()) {
            try {
                var msg = receiver.getBroadcastQueue().take();
                socket.writeLine(msg.encoded());
            } catch (InterruptedException | IOException ignored) {}
            Thread.yield();
        }
        log.error("disconnected from IPC server");
        System.exit(-1);
    }

    public static void open(HostPort dest, IConsensusClient receiver) {
        try {
            var client = new IpcClient(dest.connect(), receiver);
            new Thread(client::readTask).start();
            new Thread(client::writeTask).start();
        } catch (IOException e) {
            log.fatal("failed to connect to IPC server");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

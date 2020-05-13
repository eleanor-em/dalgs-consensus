package consensus.ipc;

import consensus.IConsensusClient;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The Inter-Process Communication (IPC) server acts as a client to the consensus algorithm, and provides the
 * resulting distributed ledger to an external entity connected to a port.
 */
public class IpcServer implements IConsensusClient {
//    private static final Logger log = LogManager.getLogger(IpcServer.class);
    private final List<IncomingMessage> received = new ArrayList<>();
    private final LinkedBlockingQueue<Message> toBroadcast = new LinkedBlockingQueue<>();
    private final IpcProcessManager processManager;

    public IpcServer(int id) {
        processManager = new IpcProcessManager(this, id);
    }

    void onNewProcess(IpcProcess p) {
        synchronized (received) {
            for (var message : received) {
                p.send(message);
            }
        }
    }

    void broadcast(Message msg) {
        toBroadcast.offer(msg);
    }

    @Override
    public LinkedBlockingQueue<Message> getBroadcastQueue() {
        return toBroadcast;
    }

    @Override
    public void receiveEntry(IncomingMessage message) {
        synchronized (received) {
            received.add(message);
            for (var p : processManager.getProcesses()) {
                p.send(message);
            }
        }
    }
}

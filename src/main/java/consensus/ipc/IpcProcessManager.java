package consensus.ipc;

import consensus.net.IoSocket;
import consensus.util.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IpcProcessManager {
    private static final Logger log = LogManager.getLogger(IpcProcessManager.class);
    private final List<IpcProcess> processes = new ArrayList<>();
    private final IpcServer parent;
    private final ExecutorService exec = Executors.newCachedThreadPool();
    private final int id;

    IpcProcessManager(IpcServer parent, int id) {
        this.parent = parent;
        this.id = id;

        exec.execute(this::listen);
    }

    List<IpcProcess> getProcesses() {
        synchronized (processes) {
            processes.removeIf(IpcProcess::closed);
            return new ArrayList<>(processes);
        }
    }

    void listen() {
        // Ensure we do not attempt to listen to the same port on multiple threads in debug mode
        if (ConfigManager.isDebug() && id != 0) {
            return;
        }

        try {
            int port = ConfigManager.getInt("ipcPort").orElse(14500);

            var serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                var ioSocket = new IoSocket(serverSocket.accept());
                var p = new IpcProcess(ioSocket, parent);
                exec.execute(p::readTask);
                parent.onNewProcess(p);

                synchronized (processes) {
                    processes.add(p);
                }
            }
            log.warn("IPC server socket closed");
        } catch (IOException e) {
            e.printStackTrace();
            log.fatal("IPC server socket died");
            System.exit(-1);
        }
    }
}

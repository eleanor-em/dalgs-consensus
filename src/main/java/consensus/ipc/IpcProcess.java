package consensus.ipc;

import consensus.net.IoSocket;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;

import java.io.IOException;

class IpcProcess {
    private boolean open = true;
    private final IoSocket socket;
    private final IpcServer parent;

    IpcProcess(IoSocket socket, IpcServer parent) {
        this.socket = socket;
        this.parent = parent;
    }

    void send(IncomingMessage message) {
        if (open) {
            try {
                socket.writeLine(message.encoded());
            } catch (IOException e) {
                open = false;
            }
        }
    }
    
    void readTask() {
        while (open) {
            var input = socket.readLine();
            if (input.isEmpty()) {
                open = false;
            } else {
                parent.broadcast(new Message(input.get()));
            }
            Thread.yield();
        }
    }
    
    boolean closed() {
        return !open;
    }
}

package consensus.net;

import consensus.net.service.PeerConnectService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.Scanner;

public class IoSocket {
    private static final Logger log = LogManager.getLogger(IoSocket.class);
    private final BufferedWriter writer;
    private final Scanner reader;
    private final Socket socket;

    public IoSocket(Socket socket) throws IOException {
        this.socket = socket;
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new Scanner(socket.getInputStream());
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            var msg = (e.getMessage() != null ? ": " + e.getMessage() : "");
            log.warn("error closing socket: " + e.getClass().getCanonicalName() + msg);
        }
    }

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    public void writeLine(int data) throws IOException {
        writeLine(Integer.toString(data));
    }

    public void writeLine(String data) throws IOException {
        synchronized (writer) {
            writer.write(data + "\n");
            writer.flush();
        }
    }

    public Optional<String> readLine() {
        synchronized (reader) {
            if (reader.hasNextLine()) {
                return Optional.of(reader.nextLine());
            } else {
                return Optional.empty();
            }
        }
    }
}

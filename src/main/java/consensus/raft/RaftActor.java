package consensus.raft;

import consensus.net.Actor;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;

/**
 * Stub class
 */
public class RaftActor extends Actor {
    private final int id;

    public RaftActor(int id) {
        this.id = id;
    }

    protected void task() throws InterruptedException {
        while (!Thread.interrupted()) {
            // Generate some random stuff to send as a demo
            Thread.sleep((int) (Math.random() * 10000));
            var num = (int) (Math.random() * 10000);
            var msg = new Message(String.format("%04x", num));

            var dest = (int) (Math.random() * 3);
            this.sendMessage(msg, dest);
            this.sendMessageToAll(msg);
            System.out.println(id + ": send (double to " + dest + ")");
        }
    }

    @Override
    protected void onReceive(IncomingMessage message) {
        System.out.println(id + ": received `" + message.msg + "` from " + message.src);
    }
}

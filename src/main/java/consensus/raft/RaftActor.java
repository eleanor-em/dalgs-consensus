package consensus.raft;

import consensus.net.Actor;
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
                Thread.sleep((int) (Math.random() * 10000));
                this.sendMessage(new Message("message from " + id));
                System.out.println(id + ": send");
            }
    }

    @Override
    protected void onReceive(Message message) {
        System.out.println(id + " received " + message);
    }
}

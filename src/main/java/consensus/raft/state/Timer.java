package consensus.raft.state;

class Timer {
    private long ms;
    private long timestamp;

    Timer() {
        reset();
    }

    public synchronized void reset() {
        this.timestamp = System.currentTimeMillis();
        this.ms = (long) (1500 + Math.random() * 1500);
    }

    public boolean expired() {
        return System.currentTimeMillis() > timestamp + ms;
    }
}

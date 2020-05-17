package consensus.raft.state;

class Timer {
    public static final int TIME_SCALE = 1;
    private long ms;
    private long timestamp;

    Timer() {
        reset();
    }

    public synchronized void reset() {
        this.timestamp = System.currentTimeMillis();
        this.ms = (long) (150 * TIME_SCALE + Math.random() * 150 * TIME_SCALE);
    }

    public boolean expired() {
        return System.currentTimeMillis() > timestamp + ms;
    }
}

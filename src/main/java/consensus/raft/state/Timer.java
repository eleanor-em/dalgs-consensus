package consensus.raft.state;

import consensus.util.ConfigManager;

class Timer {
    public static final int TIME_SCALE = 1;
    private long ms;
    private long timestamp;
    private final int raftDelay = ConfigManager.getInt("raftDelay").orElse(150);

    Timer() {
        reset();
    }

    public synchronized void reset() {
        this.timestamp = System.currentTimeMillis();
        this.ms = (long) (raftDelay * TIME_SCALE + Math.random() * raftDelay * TIME_SCALE);
    }

    public boolean expired() {
        return System.currentTimeMillis() > timestamp + ms;
    }
}

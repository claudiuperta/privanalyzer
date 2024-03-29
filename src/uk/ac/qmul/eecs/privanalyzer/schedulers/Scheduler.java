package uk.ac.qmul.eecs.privanalyzer.schedulers;

public interface Scheduler {

    public abstract void start();

    public abstract void stop();
    
    public abstract boolean isRunning();
    
    public abstract void measurementFinished(boolean measurementFailed);
}

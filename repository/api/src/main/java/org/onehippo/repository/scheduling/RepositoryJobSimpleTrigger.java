package org.onehippo.repository.scheduling;

import java.util.Date;

/**
 * Repository job trigger that allows to schedule a repository job either once at
 * a given date, or repeatedly from a given date onwards.
 */
public class RepositoryJobSimpleTrigger extends RepositoryJobTrigger {

    /**
     * Special repeat count value for indefinite repetition
     * of the trigger.
     */
    public static final int REPEAT_INDEFINITELY = -1;

    private final Date startTime;
    private final int repeatCount;
    private final long repeatInterval;

    /**
     * Create a trigger that will occur once at <code>startTime</code>.
     *
     * @param name  the name of the trigger
     * @param startTime  when the trigger must occur
     */
    public RepositoryJobSimpleTrigger(final String name, final Date startTime) {
        this(name, startTime, 0, -1);
    }

    /**
     * Create a trigger that will start at <code>startTime</code> and repeat at <code>repeatInterval</code>
     * <code>repeatCount</code> times.
     *
     * @param name  the name of the trigger.
     * @param startTime  when the first trigger must occur.
     * @param repeatCount  how many times in total the trigger must occur.
     * @param repeatInterval  the number of ms between trigger occurrences.
     */
    public RepositoryJobSimpleTrigger(final String name, final Date startTime, final int repeatCount, final long repeatInterval) {
        super(name);
        this.startTime = startTime;
        this.repeatCount = repeatCount;
        this.repeatInterval = repeatInterval;
    }

    /**
     * When the trigger will occur (for the first time).
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * How many times to repeat the trigger.
     */
    public int getRepeatCount() {
        return repeatCount;
    }

    /**
     * How long in between trigger occurrences.
     */
    public long getRepeatInterval() {
        return repeatInterval;
    }

}

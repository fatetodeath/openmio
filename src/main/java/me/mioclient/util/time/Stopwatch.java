package me.mioclient.util.time;

import java.util.concurrent.TimeUnit;

public class Stopwatch {
    public long time = -1;

    
    
    public static /* synthetic */ class Inner {
        public static final /* synthetic */ int[] intArr = new int[TimeUnit.values().length];

        static {
            try {
                intArr[TimeUnit.NANOSECONDS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                intArr[TimeUnit.MICROSECONDS.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                intArr[TimeUnit.MILLISECONDS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                intArr[TimeUnit.SECONDS.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                intArr[TimeUnit.MINUTES.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                intArr[TimeUnit.HOURS.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                intArr[TimeUnit.DAYS.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    public boolean hasElapsed(double d, TimeUnit timeUnit) {
        return hasElapsedMs(toMillis(d, timeUnit));
    }

    public boolean hasElapsedMs(long j) {
        return System.currentTimeMillis() - this.time >= j;
    }

    public boolean hasElapsedAndResetMs(long j) {
        boolean hasElapsedMs = hasElapsedMs(j);
        if (hasElapsedMs) {
            reset();
        }
        return hasElapsedMs;
    }

    public boolean hasElapsedAndReset(long j, TimeUnit timeUnit) {
        return hasElapsedAndResetMs(toMillis(j, timeUnit));
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.time;
    }

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    public void setTime(long j) {
        this.time = j;
    }

    public void setTime(double d, TimeUnit timeUnit) {
        this.time = toMillis(d, timeUnit);
    }

    public static long toMillis(double d, TimeUnit timeUnit) {
        switch (Inner.intArr[timeUnit.ordinal()]) {
            case 1:
                return (long) (d * 0.001d * 0.001d);
            case 2:
                return (long) (d * 0.001d);
            case 3:
                return (long) d;
            case 4:
                return (long) (d * 1000.0d);
            case 5:
                return (long) (d * 1000.0d * 60.0d);
            case 6:
                return (long) (d * 1000.0d * 60.0d * 60.0d);
            case 7:
                return (long) (d * 1000.0d * 60.0d * 60.0d * 24.0d);
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }
}

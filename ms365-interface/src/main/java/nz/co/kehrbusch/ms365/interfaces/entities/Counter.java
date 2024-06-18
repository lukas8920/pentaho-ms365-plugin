package nz.co.kehrbusch.ms365.interfaces.entities;

public class Counter {
    int count;

    public Counter(int count) {
        this.count = count;
    }

    public void decrement(int count) {
        this.count -= count;
    }

    public int getCount() {
        return count;
    }

    public void increment(int count) {
        this.count += count;
    }

    public Counter copy() {
        return new Counter(this.count);
    }
}

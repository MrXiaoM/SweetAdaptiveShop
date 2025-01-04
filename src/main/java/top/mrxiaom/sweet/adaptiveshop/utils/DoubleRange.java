package top.mrxiaom.sweet.adaptiveshop.utils;

import java.util.Random;

public class DoubleRange {
    private final double min;
    private final double max;
    public DoubleRange(double min, double max) {
        this.min = min;
        this.max = max;
    }
    public DoubleRange(double value) {
        this(value, value);
    }

    public double minimum() {
        return min;
    }

    public double maximum() {
        return max;
    }

    public double random() {
        if (minimum() == maximum()) return minimum();
        double rand = new Random().nextInt(1919810) / 1919809.0;
        return minimum() + (rand * (maximum() - minimum()));
    }
}

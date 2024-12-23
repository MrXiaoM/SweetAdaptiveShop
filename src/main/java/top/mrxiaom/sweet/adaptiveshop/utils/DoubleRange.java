package top.mrxiaom.sweet.adaptiveshop.utils;

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
}

package com.example.pidev.service.analytics;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class RoiCalculator {

    public static class LinearRegressionResult {
        public final double slope;
        public final double intercept;
        public final double rSquared;

        public LinearRegressionResult(double slope, double intercept, double rSquared) {
            this.slope = slope;
            this.intercept = intercept;
            this.rSquared = rSquared;
        }

        public double predict(double x) {
            return slope * x + intercept;
        }
    }

    public static LinearRegressionResult linearRegression(List<Pair<LocalDate, Double>> data) {
        if (data.size() < 2) {
            throw new IllegalArgumentException("Au moins deux points sont nécessaires");
        }
        LocalDate minDate = data.stream().map(Pair::getFirst).min(LocalDate::compareTo).orElse(LocalDate.now());
        SimpleRegression reg = new SimpleRegression();
        for (Pair<LocalDate, Double> point : data) {
            double x = ChronoUnit.DAYS.between(minDate, point.getFirst());
            double y = point.getSecond();
            reg.addData(x, y);
        }
        return new LinearRegressionResult(reg.getSlope(), reg.getIntercept(), reg.getRSquare());
    }

    public static double predict(LinearRegressionResult model, LocalDate targetDate, LocalDate minDate) {
        double x = ChronoUnit.DAYS.between(minDate, targetDate);
        return model.predict(x);
    }

    public static class Pair<U, V> {
        private final U first;
        private final V second;
        public Pair(U first, V second) { this.first = first; this.second = second; }
        public U getFirst() { return first; }
        public V getSecond() { return second; }
    }
}
package org.datatraining4j.aws.stream;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.datatraining4j.aws.stream.model.MetricEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SumAggregateFunction implements AggregateFunction<MetricEvent, Double, Double> {

    private static final Logger logger = LoggerFactory.getLogger(SumAggregateFunction.class);

    @Override
    public Double createAccumulator() {
        logger.info("Creating new accumulator");
        return 0.0;
    }

    @Override
    public Double add(MetricEvent value, Double accumulator) {
        double newAccumulator = accumulator + value.getValue();
        logger.info("Adding value: {} to accumulator: {} => New accumulator: {}", value.getValue(), accumulator, newAccumulator);
        return newAccumulator;
    }

    @Override
    public Double getResult(Double accumulator) {
        logger.info("Final result for window: {}", accumulator);
        return accumulator;
    }

    @Override
    public Double merge(Double a, Double b) {
        double merged = a + b;
        logger.info("Merging accumulators: {} + {} => {}", a, b, merged);
        return merged;
    }
}
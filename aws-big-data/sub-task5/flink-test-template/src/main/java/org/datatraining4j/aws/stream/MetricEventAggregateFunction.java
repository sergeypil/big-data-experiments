package org.datatraining4j.aws.stream;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.datatraining4j.aws.stream.model.AggregatedResult;
import org.datatraining4j.aws.stream.model.MetricEvent;

public class MetricEventAggregateFunction implements AggregateFunction<MetricEvent, AggregatedResult, AggregatedResult> {

    @Override
    public AggregatedResult createAccumulator() {
        return new AggregatedResult(null, null, 0, "", 0, null, null);
    }

    @Override
    public AggregatedResult add(MetricEvent value, AggregatedResult accumulator) {
        if (accumulator.getComponentName() == null) {
            accumulator.setComponentName(value.getComponentName());
            accumulator.setMetricName(value.getMetricName());
            accumulator.setUnit(value.getUnit());
            accumulator.setFromTimestamp(value.getPublicationTimestamp());
            accumulator.setToTimestamp(value.getPublicationTimestamp());
        }
        
        if (value.getValue() > accumulator.getMaxValue()) {
            accumulator.setMaxValue(value.getValue());
            accumulator.setToTimestamp(value.getPublicationTimestamp());
        }
        if (value.getValue() > 0.0 && accumulator.getMinValue() == 0
            || value.getValue() < accumulator.getMinValue() && accumulator.getMinValue() != 0) {
            accumulator.setMinValue(value.getValue());
            accumulator.setFromTimestamp(value.getPublicationTimestamp());
        }

        return accumulator;
    }

    @Override
    public AggregatedResult getResult(AggregatedResult accumulator) {
        return accumulator;
    }

    @Override
    public AggregatedResult merge(AggregatedResult a, AggregatedResult b) {
        throw new UnsupportedOperationException("Merge not supported");
    }
}
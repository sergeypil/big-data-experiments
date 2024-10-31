package org.datatraining4j.aws.stream;

import org.datatraining4j.aws.stream.model.MetricEvent;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SumAggregateFunctionTest {

    @Test
    public void testCreateAccumulator() {
        // Given: a sum aggregate function
        SumAggregateFunction function = new SumAggregateFunction();

        // When: creating a new accumulator
        Double accumulator = function.createAccumulator();

        // Then: the accumulator should be initialized to zero
        assertEquals(Double.valueOf(0.0), accumulator);
    }

    @Test
    public void testAdd() {
        // Given: an accumulator with initial zero value and a metric event
        SumAggregateFunction function = new SumAggregateFunction();
        Double accumulator = function.createAccumulator();
        MetricEvent event = new MetricEvent();
        event.setValue(5.0);

        // When: adding an event value to the accumulator
        Double newAccumulator = function.add(event, accumulator);

        // Then: the new accumulator value should be the sum of the old value and the event value
        assertEquals(Double.valueOf(5.0), newAccumulator);
    }

    @Test
    public void testGetResult() {
        // Given: an accumulator
        SumAggregateFunction function = new SumAggregateFunction();
        Double accumulator = function.createAccumulator();

        // When: getting the result of the accumulator
        Double result = function.getResult(accumulator);

        // Then: the result should be the same as the accumulator
        assertEquals(accumulator, result);
    }

    @Test
    public void testMerge() {
        // Given: two accumulators with values
        SumAggregateFunction function = new SumAggregateFunction();
        Double a = 1.0;
        Double b = 2.0;

        // When: merging two accumulators
        Double merged = function.merge(a, b);

        // Then: the merged value should be the sum of both accumulators
        assertEquals(Double.valueOf(3.0), merged);
    }
}
package org.datatraining4j.aws.stream.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AggregatedResult {
    private String componentName;
    private String fromTimestamp;
    private double maxValue;
    private String metricName;
    private double minValue;
    private String toTimestamp;
    private String unit;
}

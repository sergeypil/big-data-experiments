package org.datatraining4j.aws.stream.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricEvent {
    private String metricName;
    private String componentName;
    private String publicationTimestamp;
    private double value;
    private String unit;
}
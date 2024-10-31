package org.datatraining4j.aws.stream.config;


import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.datatraining4j.aws.stream.model.MetricEvent;

import java.io.IOException;

public class MetricEventDeserializationSchema implements DeserializationSchema<MetricEvent> {
    
    private final ObjectMapper objectMapper;
    
    public MetricEventDeserializationSchema(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public MetricEvent deserialize(byte[] message) throws IOException {
        return objectMapper.readValue(message, MetricEvent.class);
    }

    @Override
    public boolean isEndOfStream(MetricEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<MetricEvent> getProducedType() {
        return TypeExtractor.getForClass(MetricEvent.class);
    }
}
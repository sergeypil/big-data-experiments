package org.datatraining4j.aws.stream;

import java.io.Serializable;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.datatraining4j.aws.stream.model.TestObject;

public class JsonEncoder implements Serializable {

    private final JsonMapperFactory jsonMapperFactory;
    private transient ObjectMapper jsonMapper;

    public JsonEncoder(JsonMapperFactory jsonMapperFactory) {
        this.jsonMapperFactory = jsonMapperFactory;
    }

    public String encode(final TestObject object) {
        if (jsonMapper == null) {
            jsonMapper = jsonMapperFactory.get();
        }

        try {
            return jsonMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("failed to serialise as JSON", e);
        }
    }

    public interface JsonMapperFactory extends Serializable {

        ObjectMapper get();

    }

}

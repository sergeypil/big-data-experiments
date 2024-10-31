package org.datatraining4j.aws.stream;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.AmazonKinesisException;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Slf4j
public class KinesisStreamManager {

    @Getter
    private final String streamName;
    private final AmazonKinesis kinesis;
    private final ObjectMapper payloadMapper;

    public KinesisStreamManager(@NonNull String streamName,
                                @NonNull AmazonKinesis kinesis,
                                @NonNull ObjectMapper payloadMapper) {
        this.streamName = streamName;
        this.kinesis = kinesis;
        this.payloadMapper = payloadMapper;

        createStream();
    }

    private void createStream() {
        notBlank(streamName, "stream name required");

        try {
            kinesis.createStream(streamName, 1);
            var streamInfo = kinesis.describeStream(streamName);
            while (streamInfo.getStreamDescription().getStreamStatus().equalsIgnoreCase("creating")) {
                log.info("stream " + streamName + " is still being created");
                Thread.sleep(100);
                streamInfo = kinesis.describeStream(streamName);
            }
        } catch (AmazonKinesisException | InterruptedException e) {
            throw new RuntimeException("failed to create stream " + streamName, e);
        }
    }

    public void publishEvents(Object... events) {
        for (var event : events) {
            publishEvent(event);
        }
    }

    public void publishEvent(Object event) {
        notNull(event, "event required");

        var request = new PutRecordRequest();
        request.setStreamName(streamName);
        request.setPartitionKey("1");
        request.setData(serialise(event));

        try {
            kinesis.putRecord(request);
        } catch (AmazonKinesisException e) {
            throw new RuntimeException("failed to stream event", e);
        }
    }

    private ByteBuffer serialise(Object event) {
        byte[] bytes;
        try {
            bytes = payloadMapper.writeValueAsBytes(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("failed to serialize event", e);
        }
        return ByteBuffer.wrap(bytes);
    }

}

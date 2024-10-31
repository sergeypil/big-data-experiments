package org.datatraining4j.aws.stream;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer;
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisProducer;
import org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants;
import org.datatraining4j.aws.stream.config.KinesisSourceConfig;
import org.datatraining4j.aws.stream.config.MetricEventDeserializationSchema;
import org.datatraining4j.aws.stream.model.AggregatedResult;
import org.datatraining4j.aws.stream.model.MetricEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class StreamApplication {

    private static final Logger logger = LoggerFactory.getLogger(StreamApplication.class);

    @NonNull
    private final KinesisSourceConfig sourceConfig;

    public JobID run() throws Exception {
        var env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<MetricEvent> source = createSource(env);
        source = source.map(event -> {
            logger.info("Received event: {}", event);
            return event;
        });

        source = source
            .assignTimestampsAndWatermarks(
                WatermarkStrategy.<MetricEvent>forBoundedOutOfOrderness(Duration.ofSeconds(10))
                                                 .withTimestampAssigner((event, recordTimestamp) -> {
                                                     long timestamp = Instant.parse(event.getPublicationTimestamp()).toEpochMilli();
//                                                     logger.info("Event timestamp: {}", timestamp);
                                                     return timestamp;
                                                 })
                    .withIdleness(Duration.ofSeconds(10))
            );
        
        SingleOutputStreamOperator<AggregatedResult> windowedData = source
            .windowAll(TumblingEventTimeWindows.of(Time.seconds(5)))
            .aggregate(new MetricEventAggregateFunction());

//        windowedData.print();
        
        FlinkKinesisProducer<AggregatedResult> producer = new FlinkKinesisProducer<>(
            new JsonSerializationSchema<>(),
            getKinesisProducerConfig()
        );
        producer.setDefaultStream("MetricsOutputDataStream");
        producer.setDefaultPartition("0");
        
        logger.info("Adding sink to producer");
        windowedData.addSink(producer);
        
//        env.execute("Execute Stream Application");
//        return null;
        return env.executeAsync().getJobID();
    }


    private DataStream<MetricEvent> createSource(StreamExecutionEnvironment env) {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfigConstants.AWS_ACCESS_KEY_ID, sourceConfig.getAccessKey());
        properties.setProperty(ConsumerConfigConstants.AWS_SECRET_ACCESS_KEY, sourceConfig.getSecretKey());
        properties.setProperty(ConsumerConfigConstants.AWS_ENDPOINT, sourceConfig.getStreamsEndpointUrl());
        properties.setProperty(ConsumerConfigConstants.STREAM_INITIAL_POSITION, ConsumerConfigConstants.InitialPosition.TRIM_HORIZON.name());

        
        return env.addSource(new FlinkKinesisConsumer<>(
            sourceConfig.getStreamName(),
            new MetricEventDeserializationSchema(new ObjectMapper()),
            properties
        ));
    }

    private Properties getKinesisProducerConfig() {
        Properties config = new Properties();
        config.setProperty(ConsumerConfigConstants.AWS_REGION, "us-east-1");
        //config.setProperty("AggregationEnabled", "false");
        config.setProperty(ConsumerConfigConstants.AWS_ACCESS_KEY_ID, sourceConfig.getAccessKey());
        config.setProperty(ConsumerConfigConstants.AWS_SECRET_ACCESS_KEY, sourceConfig.getSecretKey());
        return config;
    }
}

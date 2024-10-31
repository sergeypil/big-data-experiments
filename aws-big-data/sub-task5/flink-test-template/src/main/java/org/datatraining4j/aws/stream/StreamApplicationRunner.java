package org.datatraining4j.aws.stream;

import org.apache.flink.api.common.JobID;
import org.datatraining4j.aws.stream.config.KinesisSourceConfig;

public class StreamApplicationRunner {

    public static void main(String[] args) throws Exception {
        String accessKey = System.getenv("ACCESS_KEY");
        String secretKey = System.getenv("SECRET_KEY");
        StreamApplication app = new StreamApplication(new KinesisSourceConfig(
            secretKey,
            accessKey,
                "https://kinesis.us-east-1.amazonaws.com",
                "MetricsKinesisStream"
        ));

        app.run();
    }
}
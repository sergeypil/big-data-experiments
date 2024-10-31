package org.datatraining4j.aws.stream;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.core.execution.SavepointFormatType;
import org.apache.flink.runtime.messages.FlinkJobNotFoundException;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.test.junit5.InjectClusterClient;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.datatraining4j.aws.stream.config.KinesisSourceConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Testcontainers
@Slf4j
public class StreamApplicationTest {

    @RegisterExtension
    static final MiniClusterExtension FLINK_CLUSTER = new MiniClusterExtension(
            new MiniClusterResourceConfiguration.Builder()
                    .setNumberSlotsPerTaskManager(20)
                    .setNumberTaskManagers(1)
                    .build()
    );

    private static final DockerImageName LOCAL_STACK_IMAGE =
            DockerImageName.parse("localstack/localstack:0.11.3");
    @Container
    private static final LocalStackContainer LOCAL_STACK =
            new LocalStackContainer(LOCAL_STACK_IMAGE).withServices(LocalStackContainer.Service.KINESIS);

    private static KinesisStreamManager kinesisManager;

    @BeforeAll
    static void setUp() {
        // Flink Test Utils disable all logging by default. So, re-enable it.
        Configurator.setRootLevel(Level.INFO);

        kinesisManager = initKinesis();
    }

    private static KinesisStreamManager initKinesis() {
        // Need to disable CBOR for the use with LocalStack Kinesis Streams. Not needed for AWS.
        System.setProperty("com.amazonaws.sdk.disableCbor", "true");
        System.setProperty("org.apache.flink.kinesis.shaded.com.amazonaws.sdk.disableCbor", "true");

        var kinesis = AmazonKinesisClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        LOCAL_STACK.getEndpoint().toString(),
                        LOCAL_STACK.getRegion()
                ))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                        LOCAL_STACK.getAccessKey(),
                        LOCAL_STACK.getSecretKey()
                )))
                .build();

        return new KinesisStreamManager(
                "test-metrics",
                kinesis,
                new ObjectMapper().registerModule(new JavaTimeModule())
        );
    }

    @Test
    public void shouldRun(@InjectClusterClient ClusterClient<String> flinkClusterClient,
                          @TempDir Path tempDir) throws Exception {
        var sourceConfig = KinesisSourceConfig.builder()
                .streamsEndpointUrl(LOCAL_STACK.getEndpoint().toString())
                .accessKey(LOCAL_STACK.getAccessKey())
                .secretKey(LOCAL_STACK.getSecretKey())
                .streamName(kinesisManager.getStreamName())
                .build();

        var application = new StreamApplication(sourceConfig);
        var jobId = application.run();

        Thread.sleep(3000);
        var stopRequest = flinkClusterClient.stopWithSavepoint(jobId,
                true,
                tempDir.toAbsolutePath().toString(),
                SavepointFormatType.CANONICAL
        );
        try {
            stopRequest.get(20, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FlinkJobNotFoundException) {
                log.warn("The job " + jobId + " has been already stopped." +
                        " This is OK, if your data source is finite (e.g. a collection).");
            } else {
                throw e;
            }
        }
    }

}

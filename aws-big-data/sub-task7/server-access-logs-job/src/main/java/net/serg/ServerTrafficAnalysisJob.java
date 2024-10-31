package net.serg;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.input_file_name;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.monotonically_increasing_id;
import static org.apache.spark.sql.functions.regexp_extract;
import static org.apache.spark.sql.functions.sum;
import static org.apache.spark.sql.functions.unix_timestamp;

public class ServerTrafficAnalysisJob {

    public static void main(String[] args) {
        String accessKey = System.getenv("ACCESS_KEY");
        String secretKey = System.getenv("SECRET_KEY");
        SparkSession spark = SparkSession.builder()
                                         .appName("Server Traffic Analysis Job")
                                         .master("local[1]")
                                         .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                                         .config("spark.hadoop.fs.s3a.access.key", accessKey)
                                         .config("spark.hadoop.fs.s3a.secret.key", secretKey)
                                         .config("spark.hadoop.fs.s3a.endpoint", "s3.amazonaws.com")
                                         .config("spark.hadoop.fs.s3a.region", "us-east-1")
                                         .getOrCreate();

        JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());

        String logBucketPath = "s3a://spil-server-access-logs/";
        JavaRDD<String> logData = sc.textFile(logBucketPath);

        JavaRDD<Row> rowRDD = logData.map(line -> {
            Pattern pattern = Pattern.compile("^(.*?)\\s-\\s(.*?)\\s\"(.*?)\\s(.*?)\"\\s(\\d+)\\s\\[(.*?)\\]\\s\"(.*?)\"$");
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                String source = matcher.group(2).trim();
                String target = source.contains(" ") ? source.substring(source.lastIndexOf(" ") + 1) : source;
                return RowFactory.create(source.split(" ")[0], target, matcher.group(6)); // timestamp
            }
            return null;
        }).filter(Objects::nonNull);

        StructType schema = new StructType(new StructField[]{
            new StructField("source", DataTypes.StringType, false, Metadata.empty()),
            new StructField("target", DataTypes.StringType, false, Metadata.empty()),
            new StructField("timestamp", DataTypes.StringType, false, Metadata.empty())
        });

        Dataset<Row> logDataFrame = spark.createDataFrame(rowRDD, schema)
                                         .withColumn("fileName", input_file_name());

        Dataset<Row> recentLogs = logDataFrame.filter(
            unix_timestamp(logDataFrame.col("timestamp"), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .between(unix_timestamp(lit("2022-09-15T13:42:00.000Z"), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
                         unix_timestamp(lit("2022-09-15T13:44:00.999Z"), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
        );

        Dataset<Row> groupedData = recentLogs.groupBy("source", "fileName")
                                             .count().withColumnRenamed("count", "totalRequests");

        Dataset<Row> finalData = groupedData
            .withColumn("id", monotonically_increasing_id())
            .withColumn("target", regexp_extract(col("fileName"), "spil-server-access-logs/(.*?)-\\d{4}-\\d{2}-\\d{2}\\.txt", 1));

        Dataset<Row> finalGroupedData = finalData.groupBy("source", "target")
                                                 .agg(sum("totalRequests").alias("totalRequests"))
                                                 .withColumn("id", monotonically_increasing_id());
        finalGroupedData.write().mode(SaveMode.Overwrite).json("s3a://spil-traffic-report/data/");
        
        //finalGroupedData.show();

        spark.stop();
    }
}
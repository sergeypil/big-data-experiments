package net.serg.sparkexperiments;

import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.sql.*;

public class SparkBonusTest2 {
    private static final String CSV_URL="bonus.csv";

    public static void main(String args[]){

        SparkSession spark=SparkSession.builder().master("local[*]").getOrCreate();


        Dataset<Row> csv = spark.read().format("csv")
                                .option("sep", ",")
                                .option("inferSchema", "true")
                                .option("header", "true")
                                .load(CSV_URL);

        csv
            .filter(new FilterFunction<Row>() {
                public boolean call(Row row) throws Exception {
                    return row
                        .getAs("amount")
                        .equals(15000);
                }
            })
            .show();
        csv.printSchema();
    }
}
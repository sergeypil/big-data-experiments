package net.serg.sparkexperiments;

import org.apache.spark.sql.*;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;
import static org.apache.spark.sql.functions.callUDF;
import static org.apache.spark.sql.functions.col;

public class SparkBonusTest3 {
    private static final String CSV_URL = "bonus.csv";
    private static final String ACTUAL_DEPOSIT_FUNCTION = "getActualDepositType";
    private static final String SHORT_TERM_DEPOSIT = "Short Term Deposit";
    private static final String LONG_TERM_DEPOSIT = "Long Term Deposit";
    private static final String CURRENT_DEPOSIT = "Current Deposit";

    public static void main(String args[]) {
        SparkSession spark = SparkSession.builder().master("local[*]").getOrCreate();
        Dataset<Row> csv = spark.read().format("csv")
            .option("sep", ",")
            .option("inferSchema", "true")
            .option("header", "true")
            .load(CSV_URL);
        spark.udf().register(ACTUAL_DEPOSIT_FUNCTION, getAccurateDepositType, DataTypes.StringType);
        csv.withColumn("deposit name", callUDF(ACTUAL_DEPOSIT_FUNCTION, col("deposit type"))).show();
    }
    private static final UDF1 getAccurateDepositType = new UDF1<Integer, String>() {
        public String call(final Integer i) throws Exception {
            switch (i) {
                case 1:
                    return SHORT_TERM_DEPOSIT;
                case 2:
                    return LONG_TERM_DEPOSIT;
                case 3:
                    return CURRENT_DEPOSIT;
            }
            return null;
        }
    };
}
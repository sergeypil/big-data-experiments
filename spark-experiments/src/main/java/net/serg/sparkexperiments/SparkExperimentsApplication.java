package net.serg.sparkexperiments;

import org.apache.spark.sql.SparkSession;

//@SpringBootApplication
public class SparkExperimentsApplication {

	public static void main(String[] args) {
		//SpringApplication.run(SparkExperimentsApplication.class, args);

		SparkSession spark=SparkSession.builder().master("local[*]").getOrCreate();
	}

}

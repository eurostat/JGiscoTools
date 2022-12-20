package eu.europa.ec.eurostat.jgiscotools.parquettest;

import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

public class PTest {

	public static void main(String[] args) {

		//https://blog.contactsunny.com/data-science/how-to-generate-parquet-files-in-java
		//https://github.com/macalbert/WriteParquetJavaDemo/blob/master/src/main/java/com.instarsocial.parquet/App.java

		generateParquetFile();
	}

	private static void generateParquetFile() {
		try {
			Schema schema = parseSchema();
			Path path = new Path("~/Bureau/data.parquet");

			List<GenericData.Record> recordList = generateRecords(schema);

			try (ParquetWriter<GenericData.Record> writer = AvroParquetWriter.<GenericData.Record>builder(path)
					.withSchema(schema)
					.withCompressionCodec(CompressionCodecName.SNAPPY)
					.withRowGroupSize(ParquetWriter.DEFAULT_BLOCK_SIZE)
					.withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
					.withConf(new Configuration())
					.withValidation(false)
					.withDictionaryEncoding(false)
					.build()) {

				for (GenericData.Record record : recordList) {
					writer.write(record);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}

	private static Schema parseSchema() {
		String schemaJson = "{\"namespace\": \"org.myorganization.mynamespace\"," //Not used in Parquet, can put anything
				+ "\"type\": \"record\"," //Must be set as record
				+ "\"name\": \"myrecordname\"," //Not used in Parquet, can put anything
				+ "\"fields\": ["
				+ " {\"name\": \"myString\",  \"type\": [\"string\", \"null\"]}"
				+ ", {\"name\": \"myInteger\", \"type\": \"int\"}" //Required field
				+ ", {\"name\": \"myDateTime\", \"type\": [{\"type\": \"long\", \"logicalType\" : \"timestamp-millis\"}, \"null\"]}"
				+ " ]}";

		Schema.Parser parser = new Schema.Parser().setValidate(true);
		return parser.parse(schemaJson);
	}

	private static List<GenericData.Record> generateRecords(Schema schema) {

		List<GenericData.Record> recordList = new ArrayList<>();

		long secondsOfDay = 24 * 60 * 60;

		for(int i = 1; i <= secondsOfDay; i++) {

			GenericData.Record record = new GenericData.Record(schema);
			record.put("myInteger", i);
			record.put("myString", i + " hi world of parquet!");
			record.put("myDateTime", null);

			recordList.add(record);
		}

		return recordList;
	}

}

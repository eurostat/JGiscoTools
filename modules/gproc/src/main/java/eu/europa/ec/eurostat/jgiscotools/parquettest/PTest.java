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

	public static void main(String[] args) throws Throwable {

		//https://blog.contactsunny.com/data-science/how-to-generate-parquet-files-in-java
		//https://github.com/macalbert/WriteParquetJavaDemo/blob/master/src/main/java/com.instarsocial.parquet/App.java


		String out = "/home/juju/Bureau/data.parquet";

		Schema schema = parseSchema();
		List<GenericData.Record> recordList = generateRecords(schema);

		Path path = new Path(out);
		ParquetWriter<GenericData.Record> writer = AvroParquetWriter.<GenericData.Record>builder(path)
				.withSchema(schema)
				.withCompressionCodec(CompressionCodecName.SNAPPY)
				.withRowGroupSize(ParquetWriter.DEFAULT_BLOCK_SIZE)
				.withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
				.withConf(new Configuration())
				.withValidation(false)
				.withDictionaryEncoding(false)
				.build();

		for (GenericData.Record record : recordList)
			writer.write(record);
		
		writer.close();
	}


	private static Schema parseSchema() {
		String schemaJson = "{\"namespace\": \"ns\","
				+ "\"type\": \"record\"," //set as record
				+ "\"name\": \"na\","
				+ "\"fields\": ["
				+ "{\"name\": \"id\", \"type\": \"int\"}" //required
				+ ",{\"name\": \"text\", \"type\": [\"string\", \"null\"]}"
				+ ",{\"name\": \"mag\", \"type\": \"float\"}"
				+ " ]}";

		//System.out.println(schemaJson);

		Schema.Parser parser = new Schema.Parser().setValidate(true);
		return parser.parse(schemaJson);
	}

	private static List<GenericData.Record> generateRecords(Schema schema) {

		List<GenericData.Record> recordList = new ArrayList<>();

		for(int i = 1; i <= 1000; i++) {

			GenericData.Record record = new GenericData.Record(schema);
			record.put("id", i);
			record.put("text", i + " hi!");
			record.put("mag", i * Math.PI);

			recordList.add(record);
		}

		return recordList;
	}


	/*
	public static void main(String[] args) throws Throwable {

		//https://blog.contactsunny.com/data-science/how-to-generate-parquet-files-in-java
		//https://github.com/macalbert/WriteParquetJavaDemo/blob/master/src/main/java/com.instarsocial.parquet/App.java

		String out = "/home/juju/Bureau/data.parquet";

		Schema schema = ParquetUtil.parseSchema("{\"namespace\": \"ns\","
				+ "\"type\": \"record\"," //set as record
				+ "\"name\": \"na\","
				+ "\"fields\": ["
				+ "{\"name\": \"id\", \"type\": \"int\"}" //required
				+ ",{\"name\": \"text\", \"type\": [\"string\", \"null\"]}"
				+ ",{\"name\": \"mag\", \"type\": \"float\"}"
				+ " ]}");

		List<GenericData.Record> recordList = new ArrayList<>();

		for(int i = 1; i <= 1000; i++) {
			GenericData.Record record = new GenericData.Record(schema);
			record.put("id", i);
			record.put("text", i + " hi!");
			record.put("mag", i * Math.PI);
			recordList.add(record);
		}

		ParquetUtil.save(out, schema, recordList);
	}*/

}

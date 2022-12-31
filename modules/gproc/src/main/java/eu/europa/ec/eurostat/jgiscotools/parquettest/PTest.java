package eu.europa.ec.eurostat.jgiscotools.parquettest;

import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.ParquetUtil;

public class PTest {

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
		List<GenericData.Record> recordList = generateRecords(schema);

		ParquetUtil.save(out, schema, recordList);
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

}

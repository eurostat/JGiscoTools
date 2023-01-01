package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.io.IOException;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

/**
 * @author julien Gaffuri
 *
 */
public class ParquetUtil {

	/**
	 * @param schemaJson
	 * @return
	 */
	public static Schema parseSchema(String schemaJson) {
		/*
		"{\"namespace\": \"ns\","
				+ "\"type\": \"record\"," //set as record
				+ "\"name\": \"na\","
				+ "\"fields\": ["
				+ "{\"name\": \"id\", \"type\": \"int\"}" //required
				+ ",{\"name\": \"text\", \"type\": [\"string\", \"null\"]}"
				+ ",{\"name\": \"mag\", \"type\": \"float\"}"
				+ " ]}"
		 */
		Schema.Parser parser = new Schema.Parser().setValidate(true);
		return parser.parse(schemaJson);
	}


	/**
	 * @param out
	 * @param schema
	 * @param recs
	 * @throws IOException
	 */
	public static void save(String out, Schema schema, List<GenericData.Record> recs) {
		Path path = new Path(out);
		ParquetWriter<GenericData.Record> writer;
		try {
			//prepare writer
			writer = AvroParquetWriter.<GenericData.Record>builder(path)
					.withSchema(schema)
					.withCompressionCodec(CompressionCodecName.SNAPPY)
					.withRowGroupSize(ParquetWriter.DEFAULT_BLOCK_SIZE)
					.withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
					.withConf(new Configuration())
					.withValidation(false)
					.withDictionaryEncoding(false)
					.build();

			//write records
			for (GenericData.Record record : recs)
				writer.write(record);

			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

}

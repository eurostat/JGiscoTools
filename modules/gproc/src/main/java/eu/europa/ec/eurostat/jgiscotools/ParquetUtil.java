package eu.europa.ec.eurostat.jgiscotools;

import java.io.File;
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

	//See
	//https://www.javadoc.io/doc/org.apache.parquet/parquet-column/1.10.0/index.html
	//https://avro.apache.org/docs/1.10.0/api/java/overview-summary.html
	//https://parquet.apache.org/docs/contribution-guidelines/modules/
	//https://github.com/Parquet/parquet-compatibility/blob/master/parquet-compat/src/test/java/parquet/compat/test/ConvertUtils.java


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
	 * @param removeCRCfile
	 * @throws IOException
	 */
	public static void save(String out, Schema schema, List<GenericData.Record> recs, boolean removeCRCfile) {
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

		//remove crc file
		if(removeCRCfile)
			new File("."+out+".crc").delete();
	}

}

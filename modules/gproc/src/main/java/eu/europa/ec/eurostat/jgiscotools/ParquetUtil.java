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
	 * @param folderPath
	 * @param file
	 * @param schema
	 * @param recs
	 * @param comp
	 * @param removeCRCfile
	 * @throws IOException
	 */
	public static void save(String folderPath, String file, Schema schema, List<GenericData.Record> recs, CompressionCodecName comp, boolean removeCRCfile) {
		Path path = new Path(folderPath + file);
		ParquetWriter<GenericData.Record> writer;
		try {
			//prepare writer
			writer = AvroParquetWriter.<GenericData.Record>builder(path)
					.withSchema(schema)
					.withCompressionCodec(comp)
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
		if(removeCRCfile) {
			String crc = "." + file + ".crc";
			boolean b = new File(folderPath + crc).delete();
			if(!b) System.err.println(file + "   " + crc);
		}
	}

}

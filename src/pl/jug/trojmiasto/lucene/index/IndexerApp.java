package pl.jug.trojmiasto.lucene.index;

import java.io.IOException;

import pl.jug.trojmiasto.lucene.common.Config;

public class IndexerApp {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Indexer indexer = new Indexer(Config.INDEX_PATH);
		indexer.index(new WikipediaDataProvider(10));
	}

}
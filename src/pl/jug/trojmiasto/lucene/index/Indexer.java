package pl.jug.trojmiasto.lucene.index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;

import pl.jug.trojmiasto.lucene.common.Config;
import pl.jug.trojmiasto.lucene.model.Article;

public class Indexer {
	private String indexPath;
	private final static FieldType TEXT_TYPE = new FieldType();
	static {
		TEXT_TYPE.setIndexed(true);
		TEXT_TYPE.setStored(true);
		TEXT_TYPE.setTokenized(true);
	}
	private static final FieldType NOT_ANALYZED_TEXT_FIELD = new FieldType();
	static {
		NOT_ANALYZED_TEXT_FIELD.setIndexed(true);
		NOT_ANALYZED_TEXT_FIELD.setStored(true);
		NOT_ANALYZED_TEXT_FIELD.setTokenized(false);
	}

	public Indexer(String indexPath) {
		this.indexPath = indexPath;
	}

	public void index(WikipediaDataProvider wikipediaDataProvider) throws IOException {
		System.out.println("Zaczynam indeksowanie");
		IndexWriter indexWriter = openIndex();
		for (Article article : wikipediaDataProvider) {
			Document document = articleToDocument(article);
			indexWriter.addDocument(document);
		}
		indexWriter.close();
		System.out.println("Indeksowanie zakończone");
	}

	private IndexWriter openIndex() throws IOException {
		IndexWriterConfig conf = new IndexWriterConfig(Config.VERSION, new StandardAnalyzer(Config.VERSION));
		conf.setOpenMode(OpenMode.CREATE);
		IndexWriter indexWriter = new IndexWriter(FSDirectory.open(new File(indexPath)), conf);
		return indexWriter;
	}

	private Document articleToDocument(Article article) {
		Document document = new Document();
		document.add(new Field(Config.TITLE_FIED_NAME, article.getTitle(), TEXT_TYPE));
		document.add(new Field(Config.CONTENT_FIED_NAME, article.getContent(), TEXT_TYPE));
		document.add(new Field(Config.TIME_STRING_FIED_NAME, article.getTimeString(), NOT_ANALYZED_TEXT_FIELD));
		String category = article.getCategory();
		if (null != category) {
			document.add(new Field(Config.CATEGORY_FIED_NAME, category, NOT_ANALYZED_TEXT_FIELD));
		}
		return document;
	}

}

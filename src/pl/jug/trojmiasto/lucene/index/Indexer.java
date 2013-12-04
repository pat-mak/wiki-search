package pl.jug.trojmiasto.lucene.index;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.facet.index.FacetFields;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
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
	private IndexWriter indexWriter;
	private DirectoryTaxonomyWriter taxonomyWriter;
	private FacetFields facetFields;
	private OpenMode openMode;
	static {
		NOT_ANALYZED_TEXT_FIELD.setIndexed(true);
		NOT_ANALYZED_TEXT_FIELD.setStored(true);
		NOT_ANALYZED_TEXT_FIELD.setTokenized(false);
	}

	public Indexer(String indexPath, OpenMode openMode) {
		this.indexPath = indexPath;
		this.openMode = openMode;
	}

	public void index(WikipediaDataProvider wikipediaDataProvider) throws IOException {
		System.out.println("Zaczynam indeksowanie");
		openIndex();
		for (Article article : wikipediaDataProvider) {
			addArticle(article);
		}
		closeIndex();
		System.out.println("Indeksowanie zakończone");
	}

	private void closeIndex() throws IOException {
		indexWriter.close();
		taxonomyWriter.close();
	}

	private void addArticle(Article article) throws IOException {
		Document document = articleToDocument(article);
		List<CategoryPath> categoryPaths = new LinkedList<CategoryPath>();
		String articleCategory = article.getCategory();
		if (null != articleCategory) {
			categoryPaths.add(new CategoryPath(Config.ROOT_CAT + Config.CATEGORY_SEPARATOR + articleCategory,
					Config.CATEGORY_SEPARATOR));
			facetFields.addFields(document, categoryPaths);
		}
		indexWriter.addDocument(document);
	}

	private void openIndex() throws IOException {
		Analyzer analyzer = buildAnalyzer();
		IndexWriterConfig conf = new IndexWriterConfig(Config.VERSION, analyzer);
		
		conf.setOpenMode(openMode);
		indexWriter = new IndexWriter(FSDirectory.open(new File(indexPath)), conf);

		taxonomyWriter = new DirectoryTaxonomyWriter(
				FSDirectory.open(new File(indexPath + Config.TAXO_SUFFIX)), openMode);
		facetFields = new FacetFields(taxonomyWriter);
	}

	private Analyzer buildAnalyzer() {
		EdgeNGramAnalyzer nGramAnalyzer = new EdgeNGramAnalyzer(Config.VERSION);
		Analyzer defaultAnalyzer = new StandardAnalyzer(Config.VERSION);
		Map<String, Analyzer> analyzersMap = new HashMap<String, Analyzer>();
		analyzersMap.put(Config.TITLE_NGRAM_FIED_NAME, nGramAnalyzer);
		Analyzer analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, analyzersMap);
		return analyzer;
	}

	private Document articleToDocument(Article article) {
		Document document = new Document();
		document.add(new Field(Config.TITLE_FIED_NAME, article.getTitle(), TEXT_TYPE));
		document.add(new Field(Config.TITLE_NGRAM_FIED_NAME, article.getTitle(), TEXT_TYPE));
		document.add(new Field(Config.CONTENT_FIED_NAME, article.getContent(), TEXT_TYPE));
		document.add(new Field(Config.TIME_STRING_FIED_NAME, article.getTimeString(), NOT_ANALYZED_TEXT_FIELD));
		String category = article.getCategory();
		if (null != category) {
			document.add(new Field(Config.CATEGORY_FIED_NAME, category, NOT_ANALYZED_TEXT_FIELD));
		}
		return document;
	}

	
	public void addSingleArticle(Article article) throws IOException {
		openIndex();
		addArticle(article);
		closeIndex();
	}

}

package pl.jug.trojmiasto.lucene.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.FSDirectory;

import pl.jug.trojmiasto.lucene.common.Config;
import pl.jug.trojmiasto.lucene.model.Article;

public class Searcher {

	private static final String HL_SEPARATOR = "<span class=\"separator\">&nbsp;(...)</span>";
	private static final int FRAGMENTS = 3;
	private static final int PAGE_SIZE = 10;
	private IndexSearcher searcher;

	public Searcher() throws IOException {
		searcher = new IndexSearcher(DirectoryReader.open(FSDirectory
				.open(new File(Config.INDEX_PATH))));
	}

	public SearchResult searchPrefix(String query, int i) throws Exception {
		Query luceneQuery = new PrefixQuery(new Term(Config.TITLE_FIED_NAME,
				query));

		TopDocs topDocs = searcher.search(luceneQuery, i);

		List<Article> articles = extractArticlesFromTopDocs(topDocs, null);

		SearchResult searchResult = new SearchResult();
		searchResult.setArticles(articles);
		return searchResult;
	}

	private List<Article> extractArticlesFromTopDocs(TopDocs topDocs,
			Highlighter highlighter) throws Exception {
		List<Article> articles = new ArrayList<Article>();
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document document = searcher.doc(scoreDoc.doc);
			String content = highlightContent(document, scoreDoc.doc,
					highlighter);
			Article article = new Article(document.get(Config.TITLE_FIED_NAME),
					content, document.get(Config.CATEGORY_FIED_NAME),
					document.get(Config.TIME_STRING_FIED_NAME));
			articles.add(article);
		}
		return articles;
	}

	private String highlightContent(Document document, int doc,
			Highlighter highlighter) throws IOException, InvalidTokenOffsetsException {
		if (null == highlighter) {
			return document.get(Config.CONTENT_FIED_NAME);
		}
		TokenStream tokenStream = TokenSources.getAnyTokenStream(
				searcher.getIndexReader(), doc, Config.CONTENT_FIED_NAME,
				new StandardAnalyzer(Config.VERSION));
		return highlighter.getBestFragments(tokenStream, document.get(Config.CONTENT_FIED_NAME), FRAGMENTS, HL_SEPARATOR);
	}

	public SearchResult search(String query) throws Exception {
		Query luceneQuery;
		try {
			luceneQuery = generateAnalyzedQuery(query);
		} catch (ParseException e) {
			SearchResult searchResult = new SearchResult();
			e.printStackTrace();
			searchResult.markFailed("Niepoprawne zapytanie: " + e.getMessage());
			return searchResult;
		}

		TopDocs topDocs = searcher.search(luceneQuery, PAGE_SIZE);

		Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(),
				new QueryScorer(luceneQuery));
		List<Article> articles = extractArticlesFromTopDocs(topDocs,
				highlighter);

		SearchResult searchResult = new SearchResult();
		searchResult.setCount(topDocs.totalHits);
		searchResult.setArticles(articles);
		return searchResult;
	}

	private Query generateAnalyzedQuery(String query) throws ParseException {
		AnalyzingQueryParser titleQueryParser = new AnalyzingQueryParser(
				Config.VERSION, Config.TITLE_FIED_NAME, new StandardAnalyzer(
						Config.VERSION));
		AnalyzingQueryParser contentQueryParser = new AnalyzingQueryParser(
				Config.VERSION, Config.CONTENT_FIED_NAME, new StandardAnalyzer(
						Config.VERSION));

		Query titleLuceneQuery = titleQueryParser.parse(query);
		Query contentLuceneQuery = contentQueryParser.parse(query);
		BooleanQuery booleanQuery = new BooleanQuery();
		booleanQuery.add(titleLuceneQuery, Occur.SHOULD);
		booleanQuery.add(contentLuceneQuery, Occur.SHOULD);
		return booleanQuery;
	}
}

package pl.jug.trojmiasto.lucene.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.params.FacetSearchParams;
import org.apache.lucene.facet.search.CountFacetRequest;
import org.apache.lucene.facet.search.FacetRequest;
import org.apache.lucene.facet.search.FacetResult;
import org.apache.lucene.facet.search.FacetResultNode;
import org.apache.lucene.facet.search.FacetsAccumulator;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ReferenceManager.RefreshListener;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.FSDirectory;

import pl.jug.trojmiasto.lucene.common.Config;
import pl.jug.trojmiasto.lucene.model.Article;
import pl.jug.trojmiasto.lucene.model.Category;

public class Searcher {

	private static final int CATEGORIES_SIZE = 20;
	private static final String HL_SEPARATOR = "<span class=\"separator\">&nbsp;(...)</span>";
	private static final int FRAGMENTS = 3;
	private static final int PAGE_SIZE = 20;
	private DirectoryTaxonomyReader taxonomyReader;
	private SearcherManager searcherManager;

	public Searcher() throws IOException {
		searcherManager = new SearcherManager(FSDirectory.open(new File(Config.INDEX_PATH)), null);
		searcherManager.addListener(new RefreshListener() {

			@Override
			public void afterRefresh(boolean didRefresh) throws IOException {
				if (!didRefresh) {
					return;
				}
				DirectoryTaxonomyReader changed = DirectoryTaxonomyReader.openIfChanged(taxonomyReader);
				if (null != changed) {
					taxonomyReader.decRef();
					taxonomyReader = changed;
				}
			}

			@Override
			public void beforeRefresh() throws IOException {
			}
		});
		taxonomyReader = new DirectoryTaxonomyReader(FSDirectory.open(new File(Config.INDEX_PATH
				+ Config.TAXO_SUFFIX)));
	}

	public SearchResult searchPrefix(String query, int i) throws Exception {
		searcherManager.maybeRefresh();

		CharArraySet stopWords = new CharArraySet(Config.VERSION, 1, true);
		stopWords.add("atom");
		AnalyzingQueryParser queryParser = new AnalyzingQueryParser(Config.VERSION,
				Config.TITLE_NGRAM_FIED_NAME, new StandardAnalyzer(Config.VERSION, stopWords));

		queryParser.setDefaultOperator(Operator.AND);
		Query luceneQuery = queryParser.parse(query);

		IndexSearcher searcher = searcherManager.acquire();

		List<Article> articles = null;
		try {
			TopDocs topDocs = searcher.search(luceneQuery, i);

			articles = extractArticlesFromTopDocs(topDocs, null, searcher);
		} finally {
			searcherManager.release(searcher);
		}

		SearchResult searchResult = new SearchResult();
		searchResult.setArticles(articles);
		return searchResult;
	}

	private List<Article> extractArticlesFromTopDocs(TopDocs topDocs, Highlighter highlighter,
			IndexSearcher searcher) throws Exception {
		List<Article> articles = new ArrayList<Article>();
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document document = searcher.doc(scoreDoc.doc);
			String content = highlightContent(document, scoreDoc.doc, highlighter, searcher);
			Article article = new Article(document.get(Config.TITLE_FIED_NAME), content,
					document.get(Config.CATEGORY_FIED_NAME), document.get(Config.TIME_STRING_FIED_NAME));
			articles.add(article);
		}
		return articles;
	}

	private String highlightContent(Document document, int doc, Highlighter highlighter,
			IndexSearcher searcher) throws IOException, InvalidTokenOffsetsException {
		if (null == highlighter) {
			return document.get(Config.CONTENT_FIED_NAME);
		}
		TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), doc,
				Config.CONTENT_FIED_NAME, new StandardAnalyzer(Config.VERSION));
		return highlighter.getBestFragments(tokenStream, document.get(Config.CONTENT_FIED_NAME), FRAGMENTS,
				HL_SEPARATOR);
	}

	public SearchResult search(String query) throws Exception {
		searcherManager.maybeRefresh();
		Query luceneQuery;
		try {
			luceneQuery = generateAnalyzedQuery(query);
		} catch (ParseException e) {
			SearchResult searchResult = new SearchResult();
			e.printStackTrace();
			searchResult.markFailed("Niepoprawne zapytanie: " + e.getMessage());
			return searchResult;
		}

		Sort sort = new Sort(new SortField(Config.CATEGORY_FIED_NAME, Type.STRING, false));
		TopFieldCollector topCollector = TopFieldCollector
				.create(sort, PAGE_SIZE, false, false, false, false);
		List<Article> articles = null;
		List<Category> categories = null;
		IndexSearcher searcher = searcherManager.acquire();
		try {
			FacetsCollector facetsCollector = prepareFacetCollector(searcher);
			searcher.search(luceneQuery, MultiCollector.wrap(topCollector, facetsCollector));

			Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(luceneQuery));
			articles = extractArticlesFromTopDocs(topCollector.topDocs(), highlighter, searcher);
			categories = extractCategories(facetsCollector);
		} finally {
			searcherManager.release(searcher);
		}

		SearchResult searchResult = new SearchResult();
		searchResult.setCount(topCollector.getTotalHits());
		searchResult.setArticles(articles);
		searchResult.setCategories(categories);
		return searchResult;
	}

	private Query generateAnalyzedQuery(String query) throws ParseException {
		AnalyzingQueryParser titleQueryParser = new AnalyzingQueryParser(Config.VERSION,
				Config.TITLE_FIED_NAME, new StandardAnalyzer(Config.VERSION));
		AnalyzingQueryParser contentQueryParser = new AnalyzingQueryParser(Config.VERSION,
				Config.CONTENT_FIED_NAME, new StandardAnalyzer(Config.VERSION));

		Query titleLuceneQuery = titleQueryParser.parse(query);
		Query contentLuceneQuery = contentQueryParser.parse(query);
		titleLuceneQuery.setBoost(0f);
		contentLuceneQuery.setBoost(10f);
		BooleanQuery booleanQuery = new BooleanQuery();
		booleanQuery.add(titleLuceneQuery, Occur.SHOULD);
		booleanQuery.add(contentLuceneQuery, Occur.SHOULD);
		return booleanQuery;
	}

	private FacetsCollector prepareFacetCollector(IndexSearcher searcher) {
		CategoryPath categoryPath = new CategoryPath(Config.ROOT_CAT);
		FacetRequest facetRequests = new CountFacetRequest(categoryPath, CATEGORIES_SIZE);
		facetRequests.setDepth(2);
		FacetSearchParams facetSearchParams = new FacetSearchParams(facetRequests);
		return FacetsCollector.create(new FacetsAccumulator(facetSearchParams, searcher.getIndexReader(),
				taxonomyReader));
	}

	private List<Category> extractCategories(FacetsCollector facetsCollector) throws IOException {
		List<Category> categories = new LinkedList<Category>();
		for (FacetResult facetResult : facetsCollector.getFacetResults()) {
			List<FacetResultNode> subResults = facetResult.getFacetResultNode().subResults;
			for (FacetResultNode resultNode : subResults) {
				String category = resultNode.label.toString();
				int count = (int) resultNode.value;
				categories.add(new Category(category, count));
			}
		}
		return categories;
	}
}

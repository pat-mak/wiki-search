package pl.jug.trojmiasto.lucene.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import pl.jug.trojmiasto.lucene.common.Config;
import pl.jug.trojmiasto.lucene.model.Article;
import pl.jug.trojmiasto.lucene.model.Category;

public class Searcher {

	private IndexSearcher searcher;

	public Searcher() throws IOException {
		searcher = new IndexSearcher(DirectoryReader.open(FSDirectory
				.open(new File(Config.INDEX_PATH))));
	}

	public SearchResult searchPrefix(String query, int i) throws IOException {
		Query luceneQuery = new PrefixQuery(new Term(Config.TITLE_FIED_NAME,
				query));

		TopDocs topDocs = searcher.search(luceneQuery, i);
		
		List<Article> articles = extractArticlesFromTopDocs(topDocs);
		
		SearchResult searchResult = new SearchResult();
		searchResult.setArticles(articles);
		return searchResult;
	}

	private List<Article> extractArticlesFromTopDocs(TopDocs topDocs) throws IOException {
		List<Article> articles = new ArrayList<Article>();
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document document = searcher.doc(scoreDoc.doc);
			Article article = new Article(document.get(Config.TITLE_FIED_NAME),
					document.get(Config.CONTENT_FIED_NAME),
					document.get(Config.CATEGORY_FIED_NAME),
					document.get(Config.TIME_STRING_FIED_NAME));
			articles.add(article);

		}
		return articles;
	}

	private SearchResult fakeResults() {
		List<Article> articles = new LinkedList<Article>();
		articles.add(new Article("JUG", "Przykładowy artykuł.", "Java",
				"2013-03-12T13:38:36Z"));
		SearchResult searchResult = new SearchResult();
		searchResult.setArticles(articles);
		searchResult.setCount(1);
		searchResult.setSearchTime(3000000);
		return searchResult;
	}

	public SearchResult search(String query) {
		// TODO to są przykładowe dane
		List<Category> categories = new LinkedList<Category>();
		categories.add(new Category("root/Java", 1));
		SearchResult searchResult = fakeResults();
		searchResult.setCategories(categories);
		return searchResult;
	}
}

package pl.jug.trojmiasto.lucene.index;

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.EnwikiContentSource;
import org.apache.lucene.benchmark.byTask.utils.Config;

import pl.jug.trojmiasto.lucene.model.Article;

public class WikipediaDataProvider implements Iterable<Article> {

	private static final String WIKI_DUMP = "/home/megzi/tjug-warsztaty/wikipedia/plwiki-20130502-pages-articles.xml";
	private final int requestedArticlesNuber;
	private EnwikiContentSource source;

	public WikipediaDataProvider(int requestedArticlesNuber) {
		this.requestedArticlesNuber = requestedArticlesNuber;
	}

	@Override
	public Iterator<Article> iterator() {
		source = getContentSource();
		return new Iterator<Article>() {
			DocData docData = new DocData();
			int curretnDocument = 0;
			Pattern categoryAll = Pattern.compile("\\[\\[Kategoria:.*\\]\\]");
			Pattern categoryStart = Pattern.compile("\\[\\[Kategoria:");
			Pattern categoryStop = Pattern.compile("\\]\\].*|\\|.*|\\!.*");

			@Override
			public boolean hasNext() {
				if (curretnDocument++ > requestedArticlesNuber) {
					closeSource();
					return false;
				}
				try {
					docData = source.getNextDocData(docData);
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					closeSource();
				}
				return false;
			}

			private void closeSource() {
				try {
					source.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}

			@Override
			public Article next() {
				return new Article(docData.getTitle(), docData.getBody(), extractCategory(docData.getBody()), docData.getDate());
			}
			
			private String extractCategory(String body) {
				Matcher categoryAllMatcher = categoryAll.matcher(body);
				if (!categoryAllMatcher.find()) {
					return null;
				}
				Matcher categoryStartMatcher = categoryStart.matcher(categoryAllMatcher.group());
				if (!categoryStartMatcher.find()) {
					return null;
				}
				Matcher categoryStopMatcher = categoryStop.matcher(categoryStartMatcher.replaceFirst(""));
				if (!categoryStopMatcher.find()) {
					return null;
				}
				return categoryStopMatcher.replaceFirst("");
			}


			@Override
			public void remove() {
				throw new UnsupportedOperationException("RO Iterator");

			}

		};
	}

	private static EnwikiContentSource getContentSource() {
		EnwikiContentSource source = new EnwikiContentSource();
		source.setConfig(getConfig());
		return source;
	}

	private static Config getConfig() {
		Properties props = new Properties();
		props.setProperty("docs.file", WIKI_DUMP);
		return new Config(props);
	}

}

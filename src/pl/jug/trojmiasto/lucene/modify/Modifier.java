package pl.jug.trojmiasto.lucene.modify;

import java.io.IOException;

import org.apache.lucene.index.IndexWriterConfig.OpenMode;

import pl.jug.trojmiasto.lucene.common.Config;
import pl.jug.trojmiasto.lucene.index.Indexer;
import pl.jug.trojmiasto.lucene.model.Article;

public class Modifier {

	public boolean add(String title, String category, String content, String date) {
		Article article = new Article(title, content, category, date);
		System.out.println("Dodaje artykuł: " + article);
		Indexer indexer = new Indexer(Config.INDEX_PATH, OpenMode.APPEND);
		try {
			indexer.addSingleArticle(article);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}

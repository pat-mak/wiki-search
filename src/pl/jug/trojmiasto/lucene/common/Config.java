package pl.jug.trojmiasto.lucene.common;

import org.apache.lucene.util.Version;

public class Config {
	public static final String INDEX_PATH="/tmp/first_index";
	public static final String ROOT_CAT = "root";
	public static final String TITLE_FIED_NAME = "title";
	public static final String CONTENT_FIED_NAME = "content";
	public static final String CATEGORY_FIED_NAME = "category";
	public static final String TIME_STRING_FIED_NAME = "timeString";
	public static final String TITLE_NGRAM_FIED_NAME = "titleNGram";
	public static final Version VERSION = Version.LUCENE_43;
}

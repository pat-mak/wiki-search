package pl.jug.trojmiasto.lucene.index;

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter.Side;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

public class EdgeNGramAnalyzer extends StopwordAnalyzerBase {

	protected EdgeNGramAnalyzer(Version version) {
		super(version);
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		StandardTokenizer standardTokenizer = new StandardTokenizer(matchVersion, reader);
		TokenStream stream = new StandardFilter(matchVersion, standardTokenizer);
		stream = new LowerCaseFilter(matchVersion, stream);
		stream = new EdgeNGramTokenFilter(stream, Side.FRONT, 1, 50);

		return new TokenStreamComponents(standardTokenizer, stream);
	}

}

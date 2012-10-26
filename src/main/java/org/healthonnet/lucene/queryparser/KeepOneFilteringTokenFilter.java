package org.healthonnet.lucene.queryparser;

import java.io.IOException;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/**
 * Token filter that only keeps one token of the output.
 * @author nolan
 *
 */
public class KeepOneFilteringTokenFilter extends FilteringTokenFilter {

    private int indexToKeep = -1;
    
    public KeepOneFilteringTokenFilter(TokenStream input) {
        super(true, input);
    }
    
    public void setIndexToKeep(int indexToKeep) {
        this.indexToKeep = indexToKeep;
    }
    
    @Override
    protected boolean accept() throws IOException {
        int position = getAttribute(PositionIncrementAttribute.class).getPositionIncrement();
        return position == indexToKeep || indexToKeep == -1;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
    }
}

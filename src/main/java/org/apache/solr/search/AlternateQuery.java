package org.apache.solr.search;

public class AlternateQuery implements Cloneable {

    private StringBuilder stringBuilder;
    private int endPosition;
    
    public AlternateQuery(StringBuilder stringBuilder, int endPosition) {
        this.stringBuilder = stringBuilder;
        this.endPosition = endPosition;
    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    public void setStringBuilder(StringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }
    
    public Object clone() {
        return new AlternateQuery(new StringBuilder(stringBuilder), endPosition);
    }

    @Override
    public String toString() {
        return "AlternateQuery [stringBuilder=" + stringBuilder + ", endPosition=" + endPosition + "]";
    }
}

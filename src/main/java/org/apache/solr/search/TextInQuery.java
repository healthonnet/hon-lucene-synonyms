package org.apache.solr.search;

public class TextInQuery implements Comparable<TextInQuery> {

    private String text;
    private int endPosition;
    private int startPosition;
    
    public TextInQuery(String text, int startPosition, int endPosition) {
        this.text = text;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
    
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public int getEndPosition() {
        return endPosition;
    }
    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }
    public int getStartPosition() {
        return startPosition;
    }
    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }
    @Override
    public String toString() {
        return "TextInQuery [text=" + text + ", endPosition=" + endPosition + ", startPosition=" + startPosition + "]";
    }

    public int compareTo(TextInQuery other) {
        if (this.startPosition != other.startPosition) {
            return this.startPosition - other.startPosition;
        } else if (this.endPosition != other.endPosition) {
            return this.endPosition - other.endPosition;
        }
        return this.text.compareTo(other.text);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + endPosition;
        result = prime * result + startPosition;
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TextInQuery other = (TextInQuery) obj;
        if (endPosition != other.endPosition)
            return false;
        if (startPosition != other.startPosition)
            return false;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        return true;
    }
}

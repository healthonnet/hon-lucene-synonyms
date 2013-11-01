/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.synonyms;

/**
 * Simple POJO for representing a piece of text found in the original query or expanded using shingles/synonyms.
 * @author nolan
 *
 */
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
    public int getEndPosition() {
        return endPosition;
    }
    public int getStartPosition() {
        return startPosition;
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
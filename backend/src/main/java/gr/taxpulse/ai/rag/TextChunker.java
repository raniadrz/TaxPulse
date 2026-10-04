package gr.taxpulse.ai.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits text into overlapping windows of roughly {@code chunkSize} characters, preferring to cut
 * at paragraph, sentence or word boundaries so chunks stay semantically coherent.
 */
public final class TextChunker {

    private final int chunkSize;
    private final int overlap;

    public TextChunker(int chunkSize, int overlap) {
        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Require chunkSize > overlap >= 0");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        int length = text.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            if (end < length) {
                end = bestBreak(text, start, end);
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= length) {
                break;
            }
            // Step back by the overlap, but always make forward progress.
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    /** Latest natural boundary within the second half of the window, else the hard limit. */
    private int bestBreak(String text, int start, int hardEnd) {
        int minEnd = start + chunkSize / 2;
        for (String sep : new String[] {"\n\n", ". ", ".\n", "; ", "\n", " "}) {
            int idx = text.lastIndexOf(sep, hardEnd - sep.length());
            if (idx >= minEnd) {
                return idx + sep.length();
            }
        }
        return hardEnd;
    }
}

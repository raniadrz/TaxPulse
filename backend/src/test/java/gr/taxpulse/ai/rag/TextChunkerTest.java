package gr.taxpulse.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void shortTextIsSingleChunk() {
        assertThat(new TextChunker(500, 50).split("Σύντομο κείμενο.")).containsExactly("Σύντομο κείμενο.");
    }

    @Test
    void blankTextProducesNoChunks() {
        assertThat(new TextChunker(500, 50).split("   ")).isEmpty();
    }

    @Test
    void longTextIsSplitWithinSizeAndCoversEverything() {
        String sentence = "Η περιοδική δήλωση ΦΠΑ υποβάλλεται ηλεκτρονικά. ";
        String text = sentence.repeat(100);
        List<String> chunks = new TextChunker(300, 60).split(text);

        assertThat(chunks).hasSizeGreaterThan(5);
        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(300));
        // Cuts happen on sentence boundaries.
        assertThat(chunks.subList(0, chunks.size() - 1)).allSatisfy(c -> assertThat(c).endsWith("."));
        assertThat(String.join(" ", chunks)).contains("ηλεκτρονικά");
    }

    @Test
    void consecutiveChunksOverlap() {
        String text = "abcdefghij ".repeat(60);
        List<String> chunks = new TextChunker(200, 50).split(text);
        String tailOfFirst = chunks.get(0).substring(chunks.get(0).length() - 20);
        assertThat(chunks.get(1)).contains(tailOfFirst.trim());
    }

    @Test
    void rejectsOverlapNotSmallerThanSize() {
        assertThatThrownBy(() -> new TextChunker(100, 100)).isInstanceOf(IllegalArgumentException.class);
    }
}

package gr.taxpulse.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gr.taxpulse.ai.dto.ExtractionResponse;
import gr.taxpulse.ai.ollama.ChatModelClient;
import gr.taxpulse.ai.ollama.OllamaProperties;
import gr.taxpulse.client.entity.Client;
import gr.taxpulse.client.repository.ClientRepository;
import gr.taxpulse.obligation.entity.ObligationType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DataExtractionServiceTest {

    private final ChatModelClient chatModel = mock(ChatModelClient.class);
    private final ClientRepository clientRepository = mock(ClientRepository.class);
    private final OllamaProperties props = new OllamaProperties("http://x", "llama3.2", "nomic-embed-text",
            Duration.ofSeconds(1), Duration.ofSeconds(1), null, 0.2, 4096, new OllamaProperties.Rag(1000, 100, 4));
    private final DataExtractionService service = new DataExtractionService(chatModel, props, clientRepository);

    @Test
    void validatesAndEnrichesModelOutput() {
        var raw = new DataExtractionService.RawExtraction(List.of(
                new DataExtractionService.RawRecord("EL 094 014 201", "ΑΛΦΑ ΑΕ", new BigDecimal("1234.5"), "eur",
                        "2026-10-20", "Τιμολόγιο", "VAT", "ΦΠΑ Σεπτεμβρίου"),
                new DataExtractionService.RawRecord("123456789", null, null, null, "20/10/2026", null, "NONSENSE", null)));
        when(chatModel.generateStructured(anyString(), anyString(), eq(DataExtractionService.SCHEMA),
                eq(DataExtractionService.RawExtraction.class))).thenReturn(raw);
        Client alpha = new Client();
        alpha.setName("ΑΛΦΑ ΑΕ");
        when(clientRepository.findByAfm("094014201")).thenReturn(Optional.of(alpha));
        when(clientRepository.findByAfm(any())).thenAnswer(inv ->
                "094014201".equals(inv.getArgument(0)) ? Optional.of(alpha) : Optional.empty());

        ExtractionResponse response = service.extract("κείμενο");

        var first = response.records().get(0);
        assertThat(first.afm()).isEqualTo("094014201");
        assertThat(first.afmValid()).isTrue();
        assertThat(first.amount()).isEqualByComparingTo("1234.50");
        assertThat(first.currency()).isEqualTo("EUR");
        assertThat(first.date()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(first.obligationType()).isEqualTo(ObligationType.VAT);
        assertThat(first.matchedClient()).isNotNull();

        var second = response.records().get(1);
        assertThat(second.afmValid()).isFalse();
        assertThat(second.date()).isNull();
        assertThat(second.obligationType()).isNull();
        assertThat(response.warnings()).hasSize(2);
    }

    @Test
    void normalizesAfmVariants() {
        assertThat(DataExtractionService.normalizeAfm("EL094014201")).isEqualTo("094014201");
        assertThat(DataExtractionService.normalizeAfm("094-014-201")).isEqualTo("094014201");
        assertThat(DataExtractionService.normalizeAfm(" ")).isNull();
    }
}

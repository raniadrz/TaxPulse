package gr.taxpulse.ai.ollama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.taxpulse.ai.ollama.dto.OllamaMessage;
import gr.taxpulse.common.exception.ExternalServiceException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OllamaIntegrationServiceTest {

    private static final String BASE = "http://ollama.test";

    private MockRestServiceServer server;
    private OllamaIntegrationService service;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE)
                .messageConverters(c -> {
                    c.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    c.add(new MappingJackson2HttpMessageConverter(mapper));
                });
        server = MockRestServiceServer.bindTo(builder).build();
        OllamaProperties props = new OllamaProperties(BASE, "llama3.2", "nomic-embed-text",
                Duration.ofSeconds(1), Duration.ofSeconds(5), "5m", 0.2, 4096,
                new OllamaProperties.Rag(1000, 100, 4));
        service = new OllamaIntegrationService(builder.build(), props, mapper);
    }

    @Test
    void chatSendsNonStreamingRequestAndReturnsContent() {
        server.expect(requestTo(BASE + "/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("llama3.2"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.format").doesNotExist())
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andRespond(withSuccess("""
                        {"model":"llama3.2","message":{"role":"assistant","content":"  Γεια σας! "},"done":true}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.chat(List.of(OllamaMessage.user("Γεια")))).isEqualTo("Γεια σας!");
        server.verify();
    }

    @Test
    void structuredGenerationPassesSchemaAndParsesJson() {
        Map<String, Object> schema = Map.of("type", "object",
                "properties", Map.of("afm", Map.of("type", "string")), "required", List.of("afm"));
        server.expect(requestTo(BASE + "/api/generate"))
                .andExpect(jsonPath("$.format.type").value("object"))
                .andExpect(jsonPath("$.format.properties.afm.type").value("string"))
                .andExpect(jsonPath("$.options.temperature").value(0.0))
                .andExpect(jsonPath("$.system").value("sys"))
                .andRespond(withSuccess("""
                        {"model":"llama3.2","response":"{\\"afm\\":\\"094014201\\"}","done":true}
                        """, MediaType.APPLICATION_JSON));

        record Result(String afm) {
        }
        Result result = service.generateStructured("sys", "text", schema, Result.class);
        assertThat(result.afm()).isEqualTo("094014201");
    }

    @Test
    void invalidStructuredOutputIsReported() {
        server.expect(requestTo(BASE + "/api/generate"))
                .andRespond(withSuccess("{\"response\":\"not json\",\"done\":true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.generateStructured("s", "p", Map.of("type", "object"), Map.class))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("JSON");
    }

    @Test
    void missingModelGivesActionableError() {
        server.expect(requestTo(BASE + "/api/chat")).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> service.chat(List.of(OllamaMessage.user("x"))))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("ollama pull llama3.2");
    }

    @Test
    void embedReturnsOneVectorPerInput() {
        server.expect(requestTo(BASE + "/api/embed"))
                .andExpect(jsonPath("$.model").value("nomic-embed-text"))
                .andExpect(jsonPath("$.input.length()").value(2))
                .andRespond(withSuccess("""
                        {"model":"nomic-embed-text","embeddings":[[0.1,0.2],[0.3,0.4]]}
                        """, MediaType.APPLICATION_JSON));

        List<float[]> vectors = service.embed(List.of("a", "b"));
        assertThat(vectors).hasSize(2);
        assertThat(vectors.get(1)).containsExactly(0.3f, 0.4f);
    }

    @Test
    void healthReportsInstalledModels() {
        server.expect(requestTo(BASE + "/api/tags"))
                .andRespond(withSuccess("""
                        {"models":[{"name":"llama3.2:latest"},{"name":"qwen2.5-coder:7b"}]}
                        """, MediaType.APPLICATION_JSON));

        OllamaHealth health = service.health();
        assertThat(health.reachable()).isTrue();
        assertThat(health.chatModelAvailable()).isTrue();
        assertThat(health.embeddingModelAvailable()).isFalse();
    }
}

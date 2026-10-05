package gr.taxpulse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.taxpulse.ai.ollama.OllamaIntegrationService;
import gr.taxpulse.ai.rag.VectorStore;
import gr.taxpulse.security.UserAccessCache;
import gr.taxpulse.support.PostgresTestContainer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * End-to-end test of the HTTP API against a real PostgreSQL/pgvector instance:
 * Flyway migrations + Hibernate schema validation + security + business flows.
 */
@SpringBootTest(properties = {
        "taxpulse.bootstrap.admin-email=admin@test.gr",
        "taxpulse.bootstrap.admin-password=Admin!Passw0rd",
        "taxpulse.reminders.enabled=false",
        "taxpulse.storage.root-path=${java.io.tmpdir}/taxpulse-it"
})
@AutoConfigureMockMvc
@Import(PostgresTestContainer.class)
class TaxPulseIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    VectorStore vectorStore;
    @Autowired
    UserAccessCache accessCache;
    @MockitoBean
    OllamaIntegrationService embeddingClient; // no Ollama in CI: embeddings are stubbed

    private String token;

    @BeforeEach
    void login() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.gr\",\"password\":\"Admin!Passw0rd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        token = json.readTree(body).get("accessToken").asText();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req) {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode createClient(String afm, String name) throws Exception {
        String body = """
                {"clientType":"LEGAL_ENTITY","afm":"%s","doy":"ΔΟΥ ΦΑΕ Αθηνών","name":"%s",
                 "legalForm":"ΑΕ","bookCategory":"C","email":"info@example.gr",
                 "address":{"street":"Σταδίου 10","city":"Αθήνα","postalCode":"105 64"},
                 "activityCodes":[{"code":"69.20.10.01","description":"Λογιστικές υπηρεσίες"},{"code":"62010000"}],
                 "representatives":[{"fullName":"Γιώργος Παπαδόπουλος","role":"Διευθύνων Σύμβουλος","primary":true}]}
                """.formatted(afm, name);
        String res = mvc.perform(auth(post("/api/v1/clients")).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/v1/clients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"));
    }

    @Test
    void adminCanManageStaffButNotLockThemselvesOut() throws Exception {
        String me = json.readTree(mvc.perform(auth(get("/api/v1/auth/me"))).andReturn().getResponse().getContentAsString())
                .get("id").asText();
        mvc.perform(auth(put("/api/v1/users/" + me)).content("{\"fullName\":\"Admin\",\"role\":\"ACCOUNTANT\",\"active\":true}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(auth(put("/api/v1/users/" + me)).content("{\"fullName\":\"Admin\",\"role\":\"ADMIN\",\"active\":false}"))
                .andExpect(status().isUnprocessableEntity());

        String email = "assistant-" + UUID.randomUUID() + "@test.gr";
        String created = mvc.perform(auth(post("/api/v1/users")).content("""
                        {"email":"%s","fullName":"Βοηθός","role":"ASSISTANT","password":"Assist!Passw0rd"}
                        """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String assistantId = json.readTree(created).get("id").asText();

        // The assistant can log in, read clients, but cannot create users or clients.
        String assistantToken = json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Assist!Passw0rd\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/api/v1/clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + assistantToken))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + assistantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@test.gr\",\"fullName\":\"X\",\"role\":\"ADMIN\",\"password\":\"0123456789ab\"}"))
                .andExpect(status().isForbidden());

        // A role change applies to the already-issued token on its next request.
        mvc.perform(auth(put("/api/v1/users/" + assistantId)).content("{\"fullName\":\"Βοηθός\",\"role\":\"ACCOUNTANT\",\"active\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + assistantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientType\":\"INDIVIDUAL\",\"afm\":\"070000005\",\"doy\":\"Α Αθηνών\",\"name\":\"Νέος\",\"bookCategory\":\"NONE\"}"))
                .andExpect(status().isCreated()); // was 403 while the user was an ASSISTANT

        // Deactivation revokes the existing token immediately and blocks new logins.
        mvc.perform(auth(put("/api/v1/users/" + assistantId)).content("{\"fullName\":\"Βοηθός\",\"role\":\"ASSISTANT\",\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + assistantToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Assist!Passw0rd\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientLifecycleWithValidationSearchAndUpdate() throws Exception {
        JsonNode client = createClient("094014201", "ΑΛΦΑ ΛΟΓΙΣΤΙΚΗ ΑΕ");
        assertThat(client.get("activityCodes").get(0).get("code").asText()).isEqualTo("69201001");
        assertThat(client.get("activityCodes").get(0).get("primary").asBoolean()).isTrue();

        // Invalid ΑΦΜ check digit
        mvc.perform(auth(post("/api/v1/clients")).content("""
                        {"clientType":"INDIVIDUAL","afm":"094014202","doy":"Α Αθηνών","name":"X","bookCategory":"NONE"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.afm").exists());

        // Duplicate ΑΦΜ
        mvc.perform(auth(post("/api/v1/clients")).content("""
                        {"clientType":"INDIVIDUAL","afm":"094014201","doy":"Α Αθηνών","name":"X","bookCategory":"NONE"}
                        """))
                .andExpect(status().isConflict());

        // Quick search by ΑΦΜ prefix and by name fragment (ILIKE)
        mvc.perform(auth(get("/api/v1/clients?q=0940"))).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(auth(get("/api/v1/clients?q=λογιστ"))).andExpect(jsonPath("$.content[0].afm").value("094014201"));
        mvc.perform(auth(get("/api/v1/clients/by-afm/094014201"))).andExpect(jsonPath("$.name").value("ΑΛΦΑ ΛΟΓΙΣΤΙΚΗ ΑΕ"));

        // Update: switch primary ΚΑΔ and keep the other (exercises the in-place sync)
        String id = client.get("id").asText();
        mvc.perform(auth(put("/api/v1/clients/" + id)).content("""
                        {"clientType":"LEGAL_ENTITY","afm":"094014201","doy":"ΔΟΥ ΦΑΕ Αθηνών","name":"ΑΛΦΑ ΑΕ",
                         "bookCategory":"C",
                         "activityCodes":[{"code":"69201001"},{"code":"62010000","primary":true}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ΑΛΦΑ ΑΕ"))
                .andExpect(jsonPath("$.activityCodes[0].code").value("62010000"))
                .andExpect(jsonPath("$.activityCodes[0].primary").value(true))
                .andExpect(jsonPath("$.representatives.length()").value(0));
    }

    @Test
    void obligationWorkflowCountersAndDashboard() throws Exception {
        JsonNode client = createClient("997645901", "ΒΗΤΑ ΕΠΕ");
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Athens"));

        String created = mvc.perform(auth(post("/api/v1/obligations")).content("""
                        {"clientId":"%s","obligationType":"VAT","title":"ΦΠΑ Σεπτεμβρίου",
                         "dueDate":"%s","amount":1520.40}
                        """.formatted(client.get("id").asText(), today.plusDays(3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_DOCS"))
                .andExpect(jsonPath("$.daysUntilDue").value(3))
                .andExpect(jsonPath("$.obligationTypeLabel").value("ΦΠΑ"))
                .andReturn().getResponse().getContentAsString();
        String obligationId = json.readTree(created).get("id").asText();

        // Already past due on creation -> OVERDUE immediately
        mvc.perform(auth(post("/api/v1/obligations")).content("""
                        {"clientId":"%s","obligationType":"APD","title":"ΑΠΔ Αυγούστου","dueDate":"%s"}
                        """.formatted(client.get("id").asText(), today.minusDays(2))))
                .andExpect(jsonPath("$.status").value("OVERDUE"));

        // Illegal manual transition -> 422
        mvc.perform(auth(patch("/api/v1/obligations/" + obligationId + "/status")).content("{\"status\":\"OVERDUE\"}"))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(auth(patch("/api/v1/obligations/" + obligationId + "/status"))
                        .content("{\"status\":\"SUBMITTED\",\"submissionRef\":\"ΑΑΔΕ-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedAt").exists());

        // CRM row counters come from the obligation module through the port
        mvc.perform(auth(get("/api/v1/clients?q=997645901")))
                .andExpect(jsonPath("$.content[0].overdueObligations").value(1))
                .andExpect(jsonPath("$.content[0].openObligations").value(0));

        mvc.perform(auth(get("/api/v1/obligations?clientId=" + client.get("id").asText() + "&status=OVERDUE")))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(auth(get("/api/v1/dashboard/stats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueObligations").isNumber())
                .andExpect(jsonPath("$.upcomingDeadlines").isArray());
    }

    @Test
    void reminderJobIsIdempotent() throws Exception {
        JsonNode client = createClient("800000002", "ΓΑΜΜΑ ΙΚΕ");
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Athens"));
        mvc.perform(auth(post("/api/v1/obligations")).content("""
                        {"clientId":"%s","obligationType":"MYDATA","title":"Διαβίβαση myDATA","dueDate":"%s"}
                        """.formatted(client.get("id").asText(), today.plusDays(1))))
                .andExpect(status().isCreated());

        String first = mvc.perform(auth(post("/api/v1/obligations/reminders/run")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(first).get("notificationsCreated").asInt()).isPositive();

        mvc.perform(auth(post("/api/v1/obligations/reminders/run")))
                .andExpect(jsonPath("$.notificationsCreated").value(0));

        // Created already past due -> OVERDUE at once, yet the overdue alert is still raised, once.
        String late = json.readTree(mvc.perform(auth(post("/api/v1/obligations")).content("""
                        {"clientId":"%s","obligationType":"APD","title":"ΑΠΔ που ξεχάστηκε","dueDate":"%s"}
                        """.formatted(client.get("id").asText(), today.minusDays(4))))
                .andExpect(jsonPath("$.status").value("OVERDUE"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(auth(post("/api/v1/obligations/reminders/run")))
                .andExpect(jsonPath("$.markedOverdue").value(0))
                .andExpect(jsonPath("$.notificationsCreated").value(1));
        mvc.perform(auth(post("/api/v1/obligations/reminders/run")))
                .andExpect(jsonPath("$.notificationsCreated").value(0));
        mvc.perform(auth(get("/api/v1/notifications?size=50")))
                .andExpect(jsonPath("$.content[?(@.type == 'DEADLINE_OVERDUE' && @.obligationId == '%s')]".formatted(late)).isNotEmpty());

        mvc.perform(auth(get("/api/v1/notifications/unread-count")))
                .andExpect(jsonPath("$.count").isNumber());
        mvc.perform(auth(get("/api/v1/notifications?unreadOnly=true")))
                .andExpect(jsonPath("$.content[0].type").exists());
    }

    @Test
    void pgVectorSimilaritySearchIsScopedByClient() throws Exception {
        UUID clientA = UUID.fromString(createClient("123456783", "ΔΕΛΤΑ ΑΕ").get("id").asText());
        UUID docId = UUID.randomUUID();
        jdbc.update("""
                insert into documents (id, client_id, original_filename, content_type, size_bytes, storage_key,
                                       checksum_sha256, ingestion_status)
                values (?, ?, 'egkyklios.pdf', 'application/pdf', 10, ?, repeat('a', 64), 'INDEXED')
                """, docId, clientA, "it/" + docId);

        float[] vatVector = unit(0);
        float[] payrollVector = unit(1);
        vectorStore.replaceChunks(docId, clientA, List.of("ΦΠΑ προθεσμίες", "Μισθοδοσία ΕΡΓΑΝΗ"),
                List.of(vatVector, payrollVector));

        var hits = vectorStore.similaritySearch(unit(0), clientA, 2);
        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).content()).isEqualTo("ΦΠΑ προθεσμίες");
        assertThat(hits.get(0).score()).isGreaterThan(0.99);
        assertThat(vectorStore.similaritySearch(unit(0), UUID.randomUUID(), 2)).isEmpty();
    }

    @Test
    void uploadedTextDocumentIsIndexedAsynchronously() throws Exception {
        String clientId = createClient("090000045", "ΕΨΙΛΟΝ ΟΕ").get("id").asText();
        when(embeddingClient.embed(anyList())).thenAnswer(inv -> {
            List<String> inputs = inv.getArgument(0);
            return inputs.stream().map(t -> unit(1)).toList();
        });
        when(embeddingClient.embed(anyString())).thenReturn(unit(1));

        MockMultipartFile file = new MockMultipartFile("file", "σημειώσεις.txt", "text/plain",
                "Ο πελάτης οφείλει ΦΠΑ 1.200 € έως 20/10/2026.".getBytes(StandardCharsets.UTF_8));
        String body = mvc.perform(multipart("/api/v1/clients/" + clientId + "/documents").file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingestionStatus").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String docId = json.readTree(body).get("id").asText();

        String status = "PENDING";
        for (int i = 0; i < 50 && !status.equals("INDEXED") && !status.equals("FAILED"); i++) {
            Thread.sleep(100);
            status = json.readTree(mvc.perform(auth(get("/api/v1/documents/" + docId)))
                    .andReturn().getResponse().getContentAsString()).get("ingestionStatus").asText();
        }
        assertThat(status).isEqualTo("INDEXED");
        assertThat(vectorStore.similaritySearch(unit(1), UUID.fromString(clientId), 3))
                .extracting(c -> c.content()).anyMatch(c -> c.contains("ΦΠΑ"));

        // Same bytes again for the same client -> 409
        mvc.perform(multipart("/api/v1/clients/" + clientId + "/documents").file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void portalAccountSeesOnlyItsOwnClientData() throws Exception {
        String ownId = createClient("777000013", "ΖΗΤΑ ΑΕ").get("id").asText();
        String otherId = createClient("777000025", "ΗΤΑ ΑΕ").get("id").asText();
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Athens"));
        for (String clientId : List.of(ownId, otherId)) {
            mvc.perform(auth(post("/api/v1/obligations")).content("""
                            {"clientId":"%s","obligationType":"VAT","title":"ΦΠΑ","dueDate":"%s","notes":"εσωτερική σημείωση"}
                            """.formatted(clientId, today.plusDays(7))))
                    .andExpect(status().isCreated());
        }
        MockMultipartFile otherFile = new MockMultipartFile("file", "allou.txt", "text/plain",
                "Έγγραφο άλλου πελάτη".getBytes(StandardCharsets.UTF_8));
        String otherDocId = json.readTree(mvc.perform(multipart("/api/v1/clients/" + otherId + "/documents").file(otherFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();

        // Portal accounts are created from the client, never through the staff user API.
        mvc.perform(auth(post("/api/v1/users")).content("""
                        {"email":"x-%s@test.gr","fullName":"X","role":"CLIENT","password":"Client!Passw0rd"}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity());
        String email = "portal-" + UUID.randomUUID() + "@test.gr";
        String account = mvc.perform(auth(post("/api/v1/clients/" + ownId + "/portal-accounts")).content("""
                        {"email":"%s","fullName":"Ζήτα Πελάτης","password":"Client!Passw0rd"}
                        """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.clientId").value(ownId))
                .andReturn().getResponse().getContentAsString();
        String accountId = json.readTree(account).get("id").asText();
        mvc.perform(auth(get("/api/v1/users")))
                .andExpect(jsonPath("$[?(@.role == 'CLIENT')]").isEmpty());
        // ...and can never be assigned office work.
        mvc.perform(auth(post("/api/v1/obligations")).content("""
                        {"clientId":"%s","obligationType":"VAT","title":"X","dueDate":"%s","assignedToId":"%s"}
                        """.formatted(ownId, today.plusDays(5), accountId)))
                .andExpect(status().isUnprocessableEntity());

        String clientToken = json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Client!Passw0rd\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("CLIENT"))
                .andReturn().getResponse().getContentAsString()).get("accessToken").asText();
        String bearer = "Bearer " + clientToken;

        // Staff API is closed to clients; the portal is closed to staff.
        for (String path : List.of("/api/v1/clients", "/api/v1/clients/" + otherId, "/api/v1/obligations",
                "/api/v1/users", "/api/v1/dashboard/stats", "/api/v1/clients/" + otherId + "/documents",
                "/api/v1/documents/" + otherDocId + "/download")) {
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/ai/documents/ask").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"ΦΠΑ;\",\"clientId\":\"%s\"}".formatted(otherId)))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/v1/portal/profile"))).andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.clientId").value(ownId));
        mvc.perform(get("/api/v1/portal/profile").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ΖΗΤΑ ΑΕ"))
                .andExpect(jsonPath("$.openObligations").value(1))
                .andExpect(jsonPath("$.notes").doesNotExist());
        mvc.perform(get("/api/v1/portal/obligations").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].notes").doesNotExist())
                .andExpect(jsonPath("$.content[0].assignedTo").doesNotExist());

        // Upload lands on the caller's own client; another client's document is invisible (404).
        MockMultipartFile ownFile = new MockMultipartFile("file", "timologio.txt", "text/plain",
                "Τιμολόγιο ΖΗΤΑ".getBytes(StandardCharsets.UTF_8));
        String ownDoc = mvc.perform(multipart("/api/v1/portal/documents").file(ownFile).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(ownId))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/v1/portal/documents").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/portal/documents/" + json.readTree(ownDoc).get("id").asText() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/portal/documents/" + otherDocId + "/download").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());

        // Deactivating the client closes its portal: the token stops working and login is refused.
        String ownJson = mvc.perform(auth(get("/api/v1/clients/" + ownId))).andReturn().getResponse().getContentAsString();
        ObjectNode deactivated = (ObjectNode) json.readTree(ownJson);
        deactivated.put("active", false);
        mvc.perform(auth(put("/api/v1/clients/" + ownId)).content(deactivated.toString())).andExpect(status().isOk());
        accessCache.evictAfterCommit(UUID.fromString(accountId)); // skip the 30s cache TTL for a deterministic test
        mvc.perform(get("/api/v1/portal/profile").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Client!Passw0rd\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
        deactivated.put("active", true);
        mvc.perform(auth(put("/api/v1/clients/" + ownId)).content(deactivated.toString())).andExpect(status().isOk());
        accessCache.evictAfterCommit(UUID.fromString(accountId));
        bearer = "Bearer " + loginAs(email, "Client!Passw0rd");

        // Deactivating the portal account revokes its token right away.
        mvc.perform(auth(put("/api/v1/clients/" + ownId + "/portal-accounts/" + accountId))
                        .content("{\"fullName\":\"Ζήτα Πελάτης\",\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/portal/profile").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountantAndClientInteractThroughPortal() throws Exception {
        String clientId = createClient("666000016", "ΘΗΤΑ ΑΕ").get("id").asText();
        String otherClientId = createClient("666000028", "ΙΩΤΑ ΑΕ").get("id").asText();
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Athens"));
        String accountantEmail = "acc-" + UUID.randomUUID() + "@test.gr";
        String accountantId = json.readTree(mvc.perform(auth(post("/api/v1/users")).content("""
                        {"email":"%s","fullName":"Λογιστής Θήτα","role":"ACCOUNTANT","password":"Accnt!Passw0rd"}
                        """.formatted(accountantEmail)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        String obligationId = json.readTree(mvc.perform(auth(post("/api/v1/obligations")).content("""
                        {"clientId":"%s","obligationType":"VAT","title":"ΦΠΑ Οκτωβρίου","dueDate":"%s","assignedToId":"%s"}
                        """.formatted(clientId, today.plusDays(10), accountantId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        String otherObligationId = json.readTree(mvc.perform(auth(post("/api/v1/obligations")).content("""
                        {"clientId":"%s","obligationType":"VAT","title":"ΦΠΑ","dueDate":"%s"}
                        """.formatted(otherClientId, today.plusDays(10))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        String clientEmail = "theta-" + UUID.randomUUID() + "@test.gr";
        mvc.perform(auth(post("/api/v1/clients/" + clientId + "/portal-accounts")).content("""
                        {"email":"%s","fullName":"Πελάτης Θήτα","password":"Client!Passw0rd"}
                        """.formatted(clientEmail)))
                .andExpect(status().isCreated());
        String accountant = "Bearer " + loginAs(accountantEmail, "Accnt!Passw0rd");
        String client = "Bearer " + loginAs(clientEmail, "Client!Passw0rd");

        // Client sends documents for the obligation -> the assignee is told, and the calendar shows it.
        MockMultipartFile invoices = new MockMultipartFile("file", "timologia.txt", "text/plain",
                "Τιμολόγια Οκτωβρίου".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/portal/documents").file(invoices).param("obligationId", obligationId)
                        .header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/notifications").header(HttpHeaders.AUTHORIZATION, accountant))
                .andExpect(jsonPath("$.content[?(@.type == 'DOCUMENT_RECEIVED')].obligationId").value(obligationId))
                .andExpect(jsonPath("$.content[?(@.type == 'DOCUMENT_RECEIVED')].clientId").value(clientId));
        mvc.perform(get("/api/v1/obligations/" + obligationId).header(HttpHeaders.AUTHORIZATION, accountant))
                .andExpect(jsonPath("$.clientDocuments").value(1));

        // Conversation in both directions, each side notified of the other's message.
        mvc.perform(post("/api/v1/portal/obligations/" + obligationId + "/messages").header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Λείπει ένα τιμολόγιο, το στέλνω αύριο.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromClient").value(true))
                .andExpect(jsonPath("$.mine").value(true));
        mvc.perform(get("/api/v1/notifications").header(HttpHeaders.AUTHORIZATION, accountant))
                .andExpect(jsonPath("$.content[?(@.type == 'MESSAGE')]").isNotEmpty());
        mvc.perform(post("/api/v1/obligations/" + obligationId + "/messages").header(HttpHeaders.AUTHORIZATION, accountant)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Εντάξει, ευχαριστούμε.\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/portal/obligations/" + obligationId + "/messages").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].mine").value(true))
                .andExpect(jsonPath("$[1].authorName").value("Λογιστής Θήτα"))
                .andExpect(jsonPath("$[1].mine").value(false));
        mvc.perform(get("/api/v1/notifications").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(jsonPath("$.content[?(@.type == 'MESSAGE')]").isNotEmpty());

        // A portal account cannot read or write another client's conversation; staff use their own route.
        mvc.perform(get("/api/v1/portal/obligations/" + otherObligationId + "/messages").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/portal/obligations/" + otherObligationId + "/messages").header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"x\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/obligations/" + obligationId + "/messages").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(status().isForbidden());

        // Workflow progress and documents shared by the office reach the client.
        mvc.perform(patch("/api/v1/obligations/" + obligationId + "/status").header(HttpHeaders.AUTHORIZATION, accountant)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUBMITTED\",\"submissionRef\":\"ΑΑΔΕ-42\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/notifications").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(jsonPath("$.content[?(@.type == 'STATUS_CHANGED')].message").value(hasItem(containsString("ΑΑΔΕ-42"))));
        MockMultipartFile receipt = new MockMultipartFile("file", "apodeiksi.txt", "text/plain",
                "Απόδειξη υποβολής".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/clients/" + clientId + "/documents").file(receipt).header(HttpHeaders.AUTHORIZATION, accountant))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/notifications").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(jsonPath("$.content[?(@.type == 'DOCUMENT_RECEIVED')]").isNotEmpty());
        mvc.perform(get("/api/v1/portal/obligations?status=SUBMITTED").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(jsonPath("$.content[0].messageCount").value(2));
    }

    private String loginAs(String email, String password) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }

    /** 768-dim one-hot vector (matches the vector(768) column). */
    private static float[] unit(int index) {
        float[] v = new float[768];
        v[index] = 1f;
        return v;
    }
}

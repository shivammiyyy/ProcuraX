package com.procurax.procurement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurax.identity.domain.Organization;
import com.procurax.identity.domain.OrganizationMember;
import com.procurax.identity.domain.Role;
import com.procurax.identity.domain.User;
import com.procurax.identity.repository.OrganizationMemberRepository;
import com.procurax.identity.repository.OrganizationRepository;
import com.procurax.identity.repository.PermissionRepository;
import com.procurax.identity.repository.RoleRepository;
import com.procurax.identity.repository.UserRepository;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.quotation.domain.Quotation;
import com.procurax.quotation.domain.QuotationItem;
import com.procurax.quotation.repository.QuotationItemRepository;
import com.procurax.quotation.repository.QuotationRepository;
import com.procurax.rfq.domain.Rfq;
import com.procurax.rfq.domain.RfqItem;
import com.procurax.rfq.repository.RfqItemRepository;
import com.procurax.rfq.repository.RfqRepository;
import com.procurax.vendor.domain.Vendor;
import com.procurax.vendor.domain.VendorMembership;
import com.procurax.vendor.repository.VendorMembershipRepository;
import com.procurax.vendor.repository.VendorRepository;
import com.procurax.vendor.service.DocumentStorage;
import com.procurax.vendor.service.StoredDocument;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ProcurementApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired OrganizationMemberRepository memberRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PermissionRepository permissionRepository;
    @Autowired VendorRepository vendorRepository;
    @Autowired VendorMembershipRepository vendorMembershipRepository;
    @Autowired RfqRepository rfqRepository;
    @Autowired RfqItemRepository rfqItemRepository;
    @Autowired QuotationRepository quotationRepository;
    @Autowired QuotationItemRepository quotationItemRepository;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean DocumentStorage documentStorage;

    private Organization organization;
    private User buyer;
    private OAuth2AuthenticationToken buyerToken;

    @BeforeEach
    void setUp() {
        organization = organizationRepository.save(new Organization(
                "Procurement Org", "procurement-" + UUID.randomUUID()));
        buyer = userRepository.save(new User(
                "buyer-" + UUID.randomUUID() + "@example.com", "Buyer", "google", UUID.randomUUID().toString()));
        Role role = roleRepository.findByName("ORG_ADMIN").orElseThrow();
        memberRepository.save(new OrganizationMember(organization.getId(), buyer.getId(), role.getId()));
        buyerToken = token(buyer, organization, "ORG_ADMIN", permissionRepository
                .findPermissionNamesByRoleId(role.getId()).toArray(String[]::new));
    }

    @Test
    void draftsContractOnlyFromAcceptedQuotationAndScopesReadsToOrganization() throws Exception {
        Vendor vendor = vendorRepository.save(new Vendor(
                organization.getId(), "Contract Supplier", "legal@example.test", "Facilities", 30));
        Rfq rfq = rfqRepository.save(new Rfq(organization.getId(), "Office chairs",
                "Ergonomic chairs", "RFQ", "Facilities", BigDecimal.valueOf(1000), "INR",
                Instant.now().plusSeconds(3600), 30, UUID.randomUUID()));
        Quotation quotation = new Quotation(organization.getId(), rfq.getId(), vendor.getId(),
                BigDecimal.valueOf(900), "INR", 20, 30, null, objectMapper.readTree("{}"), UUID.randomUUID());
        quotation.updateStatus("ACCEPTED");
        quotation = quotationRepository.save(quotation);

        MvcResult result = mvc.perform(post("/api/v1/contracts")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quotationId":"%s","content":"Supply agreement draft",
                                 "startDate":"2026-11-01","endDate":"2027-10-31"}
                                """.formatted(quotation.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.auditStatus").value("PENDING"))
                .andExpect(jsonPath("$.vendorName").value("Contract Supplier"))
                .andReturn();
        String contractId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();

        mvc.perform(get("/api/v1/contracts").with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(contractId));
        mvc.perform(get("/api/v1/contracts/{id}", contractId).with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotationId").value(quotation.getId().toString()));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_type = 'CONTRACT' AND aggregate_id = ?
                  AND event_type = 'CONTRACT_DRAFT_CREATED'
                """, Integer.class, UUID.fromString(contractId))).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events
                WHERE resource_type = 'CONTRACT' AND resource_id = ?
                """, Integer.class, contractId)).isEqualTo(1);

        Organization foreignOrganization = organizationRepository.save(new Organization(
                "Foreign Contract Org", "contract-foreign-" + UUID.randomUUID()));
        User foreignUser = userRepository.save(new User(
                "contract-foreign-" + UUID.randomUUID() + "@example.com", "Foreign User", "google",
                UUID.randomUUID().toString()));
        Role adminRole = roleRepository.findByName("ORG_ADMIN").orElseThrow();
        memberRepository.save(new OrganizationMember(foreignOrganization.getId(), foreignUser.getId(),
                adminRole.getId()));
        OAuth2AuthenticationToken foreignToken = token(foreignUser, foreignOrganization, "ORG_ADMIN",
                permissionRepository.findPermissionNamesByRoleId(adminRole.getId()).toArray(String[]::new));
        mvc.perform(get("/api/v1/contracts/{id}", contractId).with(authentication(foreignToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void contractDraftRejectsUnacceptedQuoteAndInvalidDates() throws Exception {
        Vendor vendor = vendorRepository.save(new Vendor(
                organization.getId(), "Pending Supplier", null, "Facilities", 30));
        Rfq rfq = rfqRepository.save(new Rfq(organization.getId(), "Tables",
                "Meeting tables", "RFQ", "Facilities", BigDecimal.valueOf(500), "INR",
                Instant.now().plusSeconds(3600), 15, UUID.randomUUID()));
        Quotation quotation = quotationRepository.save(new Quotation(organization.getId(), rfq.getId(),
                vendor.getId(), BigDecimal.valueOf(400), "INR", 10, 30, null,
                objectMapper.readTree("{}"), UUID.randomUUID()));

        mvc.perform(post("/api/v1/contracts")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quotationId":"%s","content":"Terms",
                                 "startDate":"2026-11-01","endDate":"2027-10-31"}
                                """.formatted(quotation.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUOTATION_NOT_ACCEPTED"));

        quotation.updateStatus("ACCEPTED");
        quotationRepository.save(quotation);
        mvc.perform(post("/api/v1/contracts")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quotationId":"%s","content":"Terms",
                                 "startDate":"2027-10-31","endDate":"2026-11-01"}
                                """.formatted(quotation.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CONTRACT_DATES"));
    }

    @Test
    void contractDocumentUploadRequiresValidFileAndReturnsNoStorageUrl() throws Exception {
        Vendor vendor = vendorRepository.save(new Vendor(
                organization.getId(), "Document Supplier", null, "Facilities", 30));
        Rfq rfq = rfqRepository.save(new Rfq(organization.getId(), "Desks",
                "Office desks", "RFQ", "Facilities", BigDecimal.valueOf(800), "INR",
                Instant.now().plusSeconds(3600), 20, UUID.randomUUID()));
        Quotation quotation = new Quotation(organization.getId(), rfq.getId(), vendor.getId(),
                BigDecimal.valueOf(700), "INR", 12, 30, null, objectMapper.readTree("{}"), UUID.randomUUID());
        quotation.updateStatus("ACCEPTED");
        quotation = quotationRepository.save(quotation);
        MvcResult created = mvc.perform(post("/api/v1/contracts")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quotationId":"%s","content":"Desk supply agreement",
                                 "startDate":"2026-11-01","endDate":"2027-10-31"}
                                """.formatted(quotation.getId())))
                .andExpect(status().isOk())
                .andReturn();
        String contractId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/contracts/{id}/document", contractId)
                        .file(new MockMultipartFile("file", "forged.pdf", "application/pdf",
                                "not a pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .with(authentication(buyerToken)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CONTRACT_DOCUMENT_CONTENT"));
        when(documentStorage.upload(any(byte[].class), eq("agreement.pdf"), eq("raw"),
                eq("procurement/contract_documents"), eq("authenticated")))
                .thenReturn(new StoredDocument("https://res.cloudinary.com/procurax/raw/authenticated/agreement.pdf",
                        "procurement/contract_documents/agreement", "raw", "authenticated"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/contracts/{id}/document", contractId)
                        .file(new MockMultipartFile("file", "agreement.pdf", "application/pdf",
                                "%PDF-1.7 agreement".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .with(authentication(buyerToken)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("agreement.pdf"))
                .andExpect(jsonPath("$.storageUrl").doesNotExist())
                .andExpect(jsonPath("$.secureUrl").doesNotExist());
        assertThat(jdbc.queryForObject("""
                SELECT cloudinary_delivery_type FROM contracts WHERE id = ?
                """, String.class, UUID.fromString(contractId))).isEqualTo("authenticated");
        mvc.perform(get("/api/v1/contracts/{id}", contractId).with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentFileName").value("agreement.pdf"))
                .andExpect(jsonPath("$.fileUrl").doesNotExist());
        String signedDownloadUrl = "https://api.cloudinary.com/v1_1/procurax/raw/download"
                + "?signature=signed-value&expires_at=1791192600";
        when(documentStorage.createDownloadUrl(eq("procurement/contract_documents/agreement"),
                eq("pdf"), eq("raw"), eq("authenticated"), eq("agreement.pdf"), any(Instant.class)))
                .thenReturn(signedDownloadUrl);
        mvc.perform(get("/api/v1/contracts/{id}/document/download", contractId)
                        .with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value(signedDownloadUrl))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));

        Organization foreignOrganization = organizationRepository.save(new Organization(
                "Foreign Document Org", "contract-document-foreign-" + UUID.randomUUID()));
        User foreignUser = userRepository.save(new User(
                "contract-document-foreign-" + UUID.randomUUID() + "@example.com", "Foreign User",
                "google", UUID.randomUUID().toString()));
        Role adminRole = roleRepository.findByName("ORG_ADMIN").orElseThrow();
        memberRepository.save(new OrganizationMember(foreignOrganization.getId(), foreignUser.getId(),
                adminRole.getId()));
        OAuth2AuthenticationToken foreignToken = token(foreignUser, foreignOrganization, "ORG_ADMIN",
                permissionRepository.findPermissionNamesByRoleId(adminRole.getId()).toArray(String[]::new));
        mvc.perform(get("/api/v1/contracts/{id}/document/download", contractId)
                        .with(authentication(foreignToken)))
                .andExpect(status().isNotFound());

        Vendor unrelatedVendor = vendorRepository.save(new Vendor(
                organization.getId(), "Unrelated Contract Supplier", null, "Facilities", 30));
        User unrelatedVendorUser = userRepository.save(new User(
                "unrelated-contract-vendor-" + UUID.randomUUID() + "@example.com", "Vendor",
                "google", UUID.randomUUID().toString()));
        Role vendorRole = roleRepository.findByName("VENDOR").orElseThrow();
        memberRepository.save(new OrganizationMember(organization.getId(), unrelatedVendorUser.getId(),
                vendorRole.getId()));
        vendorMembershipRepository.save(new VendorMembership(
                organization.getId(), unrelatedVendor.getId(), unrelatedVendorUser.getId(), buyer.getId()));
        OAuth2AuthenticationToken unrelatedVendorToken = token(unrelatedVendorUser, organization, "VENDOR",
                permissionRepository.findPermissionNamesByRoleId(vendorRole.getId()).toArray(String[]::new));
        mvc.perform(get("/api/v1/contracts/{id}/document/download", contractId)
                        .with(authentication(unrelatedVendorToken)))
                .andExpect(status().isNotFound());

        verify(documentStorage).upload(any(byte[].class), eq("agreement.pdf"), eq("raw"),
                eq("procurement/contract_documents"), eq("authenticated"));
        verify(documentStorage, times(1)).createDownloadUrl(eq("procurement/contract_documents/agreement"),
                eq("pdf"), eq("raw"), eq("authenticated"), eq("agreement.pdf"), any(Instant.class));
        verify(documentStorage, never()).delete(anyString(), anyString(), anyString());
    }

    @Test
    void contractAuditsAreAttributedScopedAndRequireReviewPermission() throws Exception {
        Vendor vendor = vendorRepository.save(new Vendor(
                organization.getId(), "Reviewed Supplier", null, "Facilities", 30));
        Rfq rfq = rfqRepository.save(new Rfq(organization.getId(), "Lighting",
                "Office lighting", "RFQ", "Facilities", BigDecimal.valueOf(700), "INR",
                Instant.now().plusSeconds(3600), 15, UUID.randomUUID()));
        Quotation quotation = new Quotation(organization.getId(), rfq.getId(), vendor.getId(),
                BigDecimal.valueOf(600), "INR", 10, 30, null, objectMapper.readTree("{}"), UUID.randomUUID());
        quotation.updateStatus("ACCEPTED");
        quotation = quotationRepository.save(quotation);
        MvcResult created = mvc.perform(post("/api/v1/contracts")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quotationId":"%s","content":"Payment terms: net 90",
                                 "startDate":"2026-11-01","endDate":"2027-10-31"}
                                """.formatted(quotation.getId())))
                .andExpect(status().isOk()).andReturn();
        String contractId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        User buyerUser = userRepository.save(new User(
                "contract-buyer-" + UUID.randomUUID() + "@example.com", "Buyer", "google",
                UUID.randomUUID().toString()));
        Role buyerRole = roleRepository.findByName("BUYER").orElseThrow();
        memberRepository.save(new OrganizationMember(organization.getId(), buyerUser.getId(), buyerRole.getId()));
        OAuth2AuthenticationToken buyerOnlyToken = token(buyerUser, organization, "BUYER",
                permissionRepository.findPermissionNamesByRoleId(buyerRole.getId()).toArray(String[]::new));
        mvc.perform(post("/api/v1/contracts/{id}/audits", contractId)
                        .with(authentication(buyerOnlyToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"riskLevel":"HIGH","finding":"Long payment term",
                                 "clause":"Payment due net 90","recommendation":"Negotiate net 30",
                                 "confidence":0.95}
                                """))
                .andExpect(status().isForbidden());

        MvcResult audit = mvc.perform(post("/api/v1/contracts/{id}/audits", contractId)
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"riskLevel":"HIGH","finding":"Long payment term",
                                 "clause":"Payment due net 90","recommendation":"Negotiate net 30",
                                 "confidence":0.95}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.createdBy").value(buyer.getId().toString()))
                .andReturn();
        String auditId = objectMapper.readTree(audit.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(get("/api/v1/contracts/{id}/audits", contractId).with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(auditId))
                .andExpect(jsonPath("$[0].recommendation").value("Negotiate net 30"));
        mvc.perform(get("/api/v1/contracts/{id}", contractId).with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditStatus").value("COMPLETED"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_id = ? AND event_type = 'CONTRACT_AUDIT_RECORDED'
                """, Integer.class, UUID.fromString(contractId))).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events
                WHERE resource_id = ? AND action = 'CONTRACT_AUDIT_RECORDED'
                """, Integer.class, contractId)).isEqualTo(1);

        Organization foreignOrganization = organizationRepository.save(new Organization(
                "Foreign Audit Org", "audit-foreign-" + UUID.randomUUID()));
        User foreignUser = userRepository.save(new User(
                "audit-foreign-" + UUID.randomUUID() + "@example.com", "Foreign User", "google",
                UUID.randomUUID().toString()));
        Role adminRole = roleRepository.findByName("ORG_ADMIN").orElseThrow();
        memberRepository.save(new OrganizationMember(foreignOrganization.getId(), foreignUser.getId(),
                adminRole.getId()));
        OAuth2AuthenticationToken foreignToken = token(foreignUser, foreignOrganization, "ORG_ADMIN",
                permissionRepository.findPermissionNamesByRoleId(adminRole.getId()).toArray(String[]::new));
        mvc.perform(get("/api/v1/contracts/{id}/audits", contractId).with(authentication(foreignToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void contractDecisionRequiresDifferentApproverAndRejectionReason() throws Exception {
        Vendor vendor = vendorRepository.save(new Vendor(
                organization.getId(), "Decision Supplier", null, "Facilities", 30));
        Rfq rfq = rfqRepository.save(new Rfq(organization.getId(), "Desks",
                "Office desks", "RFQ", "Facilities", BigDecimal.valueOf(800), "INR",
                Instant.now().plusSeconds(3600), 20, UUID.randomUUID()));
        Quotation quotation = new Quotation(organization.getId(), rfq.getId(), vendor.getId(),
                BigDecimal.valueOf(750), "INR", 12, 30, null, objectMapper.readTree("{}"), UUID.randomUUID());
        quotation.updateStatus("ACCEPTED");
        quotation = quotationRepository.save(quotation);
        MvcResult created = mvc.perform(post("/api/v1/contracts")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quotationId":"%s","content":"Terms for review",
                                 "startDate":"2026-11-01","endDate":"2027-10-31"}
                                """.formatted(quotation.getId())))
                .andExpect(status().isOk()).andReturn();
        String contractId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        Vendor anotherVendor = vendorRepository.save(new Vendor(
                organization.getId(), "Unrelated Supplier", null, "Facilities", 30));
        User unrelatedVendorUser = userRepository.save(new User(
                "unrelated-vendor-" + UUID.randomUUID() + "@example.com", "Vendor", "google",
                UUID.randomUUID().toString()));
        Role vendorRole = roleRepository.findByName("VENDOR").orElseThrow();
        memberRepository.save(new OrganizationMember(organization.getId(), unrelatedVendorUser.getId(),
                vendorRole.getId()));
        vendorMembershipRepository.save(new VendorMembership(
                organization.getId(), anotherVendor.getId(), unrelatedVendorUser.getId(), buyer.getId()));
        OAuth2AuthenticationToken unrelatedVendorToken = token(unrelatedVendorUser, organization, "VENDOR",
                permissionRepository.findPermissionNamesByRoleId(vendorRole.getId()).toArray(String[]::new));
        mvc.perform(get("/api/v1/contracts/{id}", contractId).with(authentication(unrelatedVendorToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/contracts").with(authentication(unrelatedVendorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(post("/api/v1/contracts/{id}/decision", contractId)
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVE","comment":"Looks acceptable"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONTRACT_SELF_APPROVAL"));

        User approver = userRepository.save(new User(
                "contract-approver-" + UUID.randomUUID() + "@example.com", "Approver", "google",
                UUID.randomUUID().toString()));
        Role approverRole = roleRepository.findByName("APPROVER").orElseThrow();
        memberRepository.save(new OrganizationMember(organization.getId(), approver.getId(), approverRole.getId()));
        OAuth2AuthenticationToken approverToken = token(approver, organization, "APPROVER",
                permissionRepository.findPermissionNamesByRoleId(approverRole.getId()).toArray(String[]::new));
        mvc.perform(post("/api/v1/contracts/{id}/decision", contractId)
                        .with(authentication(approverToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"REJECT","comment":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTRACT_REJECTION_COMMENT_REQUIRED"));

        MvcResult decision = mvc.perform(post("/api/v1/contracts/{id}/decision", contractId)
                        .with(authentication(approverToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVE","comment":"Reviewed and accepted"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").value(approver.getId().toString()))
                .andReturn();
        String decisionId = objectMapper.readTree(decision.getResponse().getContentAsString())
                .get("id").asText();
        mvc.perform(get("/api/v1/contracts/{id}", contractId).with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(get("/api/v1/contracts/{id}/decisions", contractId)
                        .with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(decisionId));
        mvc.perform(post("/api/v1/contracts/{id}/decision", contractId)
                        .with(authentication(approverToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"REJECT","comment":"Changed my mind"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONTRACT_ALREADY_DECIDED"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_id = ? AND event_type = 'CONTRACT_APPROVED'
                """, Integer.class, UUID.fromString(contractId))).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events
                WHERE resource_id = ? AND action = 'CONTRACT_APPROVED'
                """, Integer.class, contractId)).isEqualTo(1);
    }

    @Test
    void replacesDraftRfqLineItemsAndRejectsEditingAfterPublish() throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/rfqs")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Office supplies","description":"Initial scope",
                                 "requestType":"RFQ","category":"Facilities","budget":1000,
                                 "currency":"INR","deadline":"%s","deliveryDays":14,
                                 "items":[{"description":"Paper","quantity":2,"unit":"box"}]}
                                """.formatted(Instant.now().plusSeconds(7200))))
                .andExpect(status().isOk()).andReturn();
        String rfqId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mvc.perform(put("/api/v1/rfqs/{id}", rfqId)
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated office supplies","description":"Revised scope",
                                 "requestType":"RFP","category":"Facilities","budget":2500,
                                 "currency":"INR","deadline":"%s","deliveryDays":21,
                                 "items":[
                                   {"description":"Recycled paper","quantity":4,"unit":"box",
                                    "specification":"A4, 80 gsm"},
                                   {"description":"Pens","quantity":12,"unit":"pack"}
                                 ]}
                                """.formatted(Instant.now().plusSeconds(10800))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated office supplies"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].description").value("Recycled paper"))
                .andExpect(jsonPath("$.items[0].specification").value("A4, 80 gsm"))
                .andExpect(jsonPath("$.items[1].quantity").value(12));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_type = 'RFQ' AND aggregate_id = ? AND event_type = 'RFQ_UPDATED'
                """, Integer.class, UUID.fromString(rfqId))).isEqualTo(1);

        mvc.perform(post("/api/v1/rfqs/{id}/publish", rfqId)
                        .with(authentication(buyerToken)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(put("/api/v1/rfqs/{id}", rfqId)
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Illegal edit","description":"Must stay published",
                                 "requestType":"RFQ","category":"Facilities","budget":2500,
                                 "currency":"INR","deadline":"%s","deliveryDays":21,
                                 "items":[{"description":"Paper","quantity":4}]}
                                """.formatted(Instant.now().plusSeconds(10800))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RFQ_NOT_EDITABLE"));
    }

    @Test
    void createsVendorAndDraftRfqWithinPrincipalOrganization() throws Exception {
        MvcResult vendorResult = mvc.perform(post("/api/v1/vendors")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Northstar Supplies","contactEmail":"sales@example.com",
                                 "category":"IT Hardware","paymentTermsDays":30,
                                 "organizationId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Northstar Supplies"))
                .andReturn();
        JsonNode vendor = objectMapper.readTree(vendorResult.getResponse().getContentAsString());

        MvcResult rfqResult = mvc.perform(post("/api/v1/rfqs")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Engineering laptops","description":"Business laptops",
                                 "requestType":"RFQ","category":"IT Hardware","budget":2500000,
                                 "currency":"INR","deadline":"%s","deliveryDays":14,
                                 "organizationId":"%s",
                                 "items":[{"description":"Laptop","quantity":20,"unit":"each",
                                           "specification":"16 GB RAM"}]}
                                """.formatted(Instant.now().plusSeconds(3600), UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].quantity").value(20))
                .andReturn();
        JsonNode rfq = objectMapper.readTree(rfqResult.getResponse().getContentAsString());
        UUID rfqId = UUID.fromString(rfq.get("id").asText());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events WHERE aggregate_type = 'RFQ' AND aggregate_id = ?
                """, Integer.class, rfqId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE resource_type = 'RFQ' AND resource_id = ?
                """, Integer.class, rfqId.toString())).isEqualTo(1);

        mvc.perform(get("/api/v1/rfqs/" + rfq.get("id").asText()).with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budget").value(2500000));
        mvc.perform(get("/api/v1/vendors/" + vendor.get("id").asText()).with(authentication(buyerToken)))
                .andExpect(status().isOk());
    }

    @Test
    void quotationAmountAndScoreAreServerCalculatedAndVendorCannotReadCompetitorQuote() throws Exception {
        Vendor vendor = vendorRepository.save(new Vendor(
                organization.getId(), "Contoso Devices", "sales@contoso.test", "IT Hardware", 30));
        User vendorUser = userRepository.save(new User(
                "vendor-" + UUID.randomUUID() + "@example.com", "Vendor User", "google",
                UUID.randomUUID().toString()));
        Role vendorRole = roleRepository.findByName("VENDOR").orElseThrow();
        memberRepository.save(new OrganizationMember(organization.getId(), vendorUser.getId(), vendorRole.getId()));

        mvc.perform(post("/api/v1/vendors/{id}/members", vendor.getId())
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s"}
                                """.formatted(vendorUser.getId())))
                .andExpect(status().isOk());

        MvcResult rfqResult = mvc.perform(post("/api/v1/rfqs")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Displays","description":"Displays for engineering",
                                 "requestType":"RFQ","category":"IT Hardware","budget":500,
                                 "currency":"INR","deadline":"%s","deliveryDays":10,
                                 "items":[{"description":"Display","quantity":2,"unit":"each"}]}
                                """.formatted(Instant.now().plusSeconds(3600))))
                .andExpect(status().isOk()).andReturn();
        JsonNode rfq = objectMapper.readTree(rfqResult.getResponse().getContentAsString());
        String rfqId = rfq.get("id").asText();
        String itemId = rfq.get("items").get(0).get("id").asText();

        mvc.perform(post("/api/v1/rfqs/{id}/publish", rfqId)
                        .with(authentication(buyerToken)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        OAuth2AuthenticationToken vendorToken = token(vendorUser, organization, "VENDOR",
                permissionRepository.findPermissionNamesByRoleId(vendorRole.getId()).toArray(String[]::new));
        String quoteRequest = """
                {"rfqId":"%s","currency":"INR","deliveryDays":8,"paymentTermsDays":30,
                 "qualityRating":4.0,
                 "compliance":{"isoCertification":true,"materialGrade":"A+",
                               "environmentalStandards":true,"documentSubmission":true},
                 "items":[{"rfqItemId":"%s","unitPrice":50,"quantity":2}],
                 "totalAmount":1}
                """.formatted(rfqId, itemId);

        MvcResult quoteResult = mvc.perform(post("/api/v1/quotations")
                        .with(authentication(vendorToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(quoteRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(100))
                .andExpect(jsonPath("$.vendorId").value(vendor.getId().toString()))
                .andExpect(jsonPath("$.vendorScore.score").value(85.00))
                .andReturn();
        JsonNode quote = objectMapper.readTree(quoteResult.getResponse().getContentAsString());

        mvc.perform(post("/api/v1/quotations")
                        .with(authentication(vendorToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(quoteRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUOTATION_ALREADY_SUBMITTED"));

        User otherVendorUser = userRepository.save(new User(
                "other-vendor-" + UUID.randomUUID() + "@example.com", "Other Vendor", "google",
                UUID.randomUUID().toString()));
        memberRepository.save(new OrganizationMember(organization.getId(), otherVendorUser.getId(), vendorRole.getId()));
        Vendor otherVendor = vendorRepository.save(new Vendor(
                organization.getId(), "Fabrikam Displays", "fabrikam@example.test", "IT Hardware", 30));
        mvc.perform(post("/api/v1/vendors/{id}/members", otherVendor.getId())
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s"}
                                """.formatted(otherVendorUser.getId())))
                .andExpect(status().isOk());
        OAuth2AuthenticationToken otherVendorToken = token(otherVendorUser, organization, "VENDOR",
                permissionRepository.findPermissionNamesByRoleId(vendorRole.getId()).toArray(String[]::new));

        mvc.perform(get("/api/v1/quotations/{id}", quote.get("id").asText())
                        .with(authentication(otherVendorToken)))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/rfqs/{id}/quotations", rfqId).with(authentication(vendorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(quote.get("id").asText()));

        String secondQuoteRequest = quoteRequest.replace("\"unitPrice\":50", "\"unitPrice\":60");
        MvcResult secondQuoteResult = mvc.perform(post("/api/v1/quotations")
                        .with(authentication(otherVendorToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(secondQuoteRequest))
                .andExpect(status().isOk()).andReturn();
        JsonNode secondQuote = objectMapper.readTree(secondQuoteResult.getResponse().getContentAsString());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/v1/quotations/{id}/status", quote.get("id").asText())
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/v1/quotations/{id}/status", secondQuote.get("id").asText())
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RFQ_QUOTE_ALREADY_ACCEPTED"));

        mvc.perform(get("/api/v1/rfqs/{id}", rfqId).with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_type = 'QUOTATION' AND event_type = 'QUOTATION_SUBMITTED'
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_type = 'QUOTATION' AND event_type = 'QUOTATION_STATUS_CHANGED'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_type = 'RFQ' AND event_type = 'RFQ_AWARD_SELECTED'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void crossOrganizationVendorIdIsNotFound() throws Exception {
        Vendor vendor = vendorRepository.save(new Vendor(
                organization.getId(), "Private Vendor", null, "IT Hardware", 30));
        Organization foreignOrganization = organizationRepository.save(new Organization(
                "Foreign Org", "foreign-" + UUID.randomUUID()));
        User foreignUser = userRepository.save(new User(
                "foreign-" + UUID.randomUUID() + "@example.com", "Foreign User", "google",
                UUID.randomUUID().toString()));
        Role buyerRole = roleRepository.findByName("BUYER").orElseThrow();
        memberRepository.save(new OrganizationMember(foreignOrganization.getId(), foreignUser.getId(), buyerRole.getId()));
        OAuth2AuthenticationToken foreignToken = token(foreignUser, foreignOrganization, "BUYER",
                permissionRepository.findPermissionNamesByRoleId(buyerRole.getId()).toArray(String[]::new));

        mvc.perform(get("/api/v1/vendors/{id}", vendor.getId()).with(authentication(foreignToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/rfqs").with(authentication(foreignToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void vendorDocumentsAreTenantScopedAndVerificationIsAuditedAndOneWay() throws Exception {
        MvcResult vendorResult = mvc.perform(post("/api/v1/vendors")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Documented Vendor","category":"IT Hardware","paymentTermsDays":30}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String vendorId = objectMapper.readTree(vendorResult.getResponse().getContentAsString())
                .get("id").asText();

        MvcResult documentResult = mvc.perform(post("/api/v1/vendors/{id}/documents", vendorId)
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentType":"TAX_CERTIFICATE","fileName":"tax.pdf",
                                 "storageUrl":"https://res.cloudinary.com/procurax/raw/upload/tax.pdf"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andReturn();
        String documentId = objectMapper.readTree(documentResult.getResponse().getContentAsString())
                .get("id").asText();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_type = 'VENDOR_DOCUMENT' AND aggregate_id = ? AND event_type = 'VENDOR_DOCUMENT_REGISTERED'
                """, Integer.class, UUID.fromString(documentId))).isEqualTo(1);

        User buyerUser = userRepository.save(new User(
                "reader-" + UUID.randomUUID() + "@example.com", "Buyer", "google",
                UUID.randomUUID().toString()));
        Role buyerRole = roleRepository.findByName("BUYER").orElseThrow();
        memberRepository.save(new OrganizationMember(organization.getId(), buyerUser.getId(), buyerRole.getId()));
        OAuth2AuthenticationToken readerToken = token(buyerUser, organization, "BUYER",
                permissionRepository.findPermissionNamesByRoleId(buyerRole.getId()).toArray(String[]::new));
        mvc.perform(patch("/api/v1/vendors/{vendorId}/documents/{documentId}/verification",
                        vendorId, documentId)
                        .with(authentication(readerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"VERIFIED"}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/vendors/{vendorId}/documents/{documentId}/verification",
                        vendorId, documentId)
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"REJECTED","rejectionReason":"Expired certificate"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.verifiedBy").value(buyer.getId().toString()))
                .andExpect(jsonPath("$.rejectionReason").value("Expired certificate"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_id = ? AND event_type = 'VENDOR_DOCUMENT_REJECTED'
                """, Integer.class, UUID.fromString(documentId))).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events
                WHERE resource_id = ? AND action = 'VENDOR_DOCUMENT_REJECTED'
                """, Integer.class, documentId)).isEqualTo(1);

        mvc.perform(patch("/api/v1/vendors/{vendorId}/documents/{documentId}/verification",
                        vendorId, documentId)
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"VERIFIED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VENDOR_DOCUMENT_ALREADY_REVIEWED"));

        mvc.perform(post("/api/v1/vendors/{id}/documents", vendorId)
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentType":"TAX_CERTIFICATE","fileName":"tax.pdf",
                                 "storageUrl":"https://example.com/tax.pdf"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DOCUMENT_STORAGE_URL"));
    }

    @Test
    void uploadsValidatedVendorDocumentToCloudinaryAndPersistsReference() throws Exception {
        MvcResult vendorResult = mvc.perform(post("/api/v1/vendors")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Upload Vendor","category":"IT Hardware","paymentTermsDays":30}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String vendorId = objectMapper.readTree(vendorResult.getResponse().getContentAsString())
                .get("id").asText();
        when(documentStorage.upload(any(byte[].class), eq("policy.pdf"), eq("raw"),
                eq("procurement/vendor_documents"), eq("upload")))
                .thenReturn(new StoredDocument(
                        "https://res.cloudinary.com/procurax/raw/upload/policy.pdf",
                        "procurement/vendor_documents/policy", "raw", "upload"));

        MvcResult uploadResult = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/vendors/{id}/documents/upload", vendorId)
                        .file(new MockMultipartFile("file", "policy.pdf", "application/pdf",
                                "%PDF-1.7 document".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .param("documentType", "POLICY")
                        .with(authentication(buyerToken)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorId").value(vendorId))
                .andExpect(jsonPath("$.fileName").value("policy.pdf"))
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andReturn();
        verify(documentStorage).upload(
                any(byte[].class), eq("policy.pdf"), eq("raw"),
                eq("procurement/vendor_documents"), eq("upload"));
        UUID uploadedDocumentId = UUID.fromString(objectMapper.readTree(
                uploadResult.getResponse().getContentAsString()).get("id").asText());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_id = ? AND event_type = 'VENDOR_DOCUMENT_UPLOADED'
                """, Integer.class, uploadedDocumentId)).isEqualTo(1);
        clearInvocations(documentStorage);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/vendors/{id}/documents/upload", vendorId)
                        .file(new MockMultipartFile("file", "forged.pdf", "application/pdf",
                                "not a pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .param("documentType", "POLICY")
                        .with(authentication(buyerToken)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DOCUMENT_CONTENT"));
        verifyNoInteractions(documentStorage);
    }

    @Test
    void organizationPoliciesEnforceLimitsCurrencyAndApprovalThresholds() throws Exception {
        mvc.perform(post("/api/v1/policies/rules")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maximum purchase","ruleType":"MAX_PURCHASE_AMOUNT",
                                 "category":"Facilities",
                                 "thresholdAmount":1000,"thresholdCurrency":"INR","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleType").value("MAX_PURCHASE_AMOUNT"));
        mvc.perform(post("/api/v1/policies/rules")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Currency allowlist","ruleType":"ALLOWED_CURRENCIES",
                                 "allowedCurrencyCodes":["INR","USD"],"enabled":true}
                                """))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/policies/rules")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Director approval","ruleType":"APPROVAL_THRESHOLD",
                                 "category":"Facilities",
                                 "thresholdAmount":500,"thresholdCurrency":"INR","enabled":true}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Facilities","amount":600,"currency":"INR",
                                 "quotationCount":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("APPROVAL_REQUIRED"))
                .andExpect(jsonPath("$.requiresApproval").value(true))
                .andExpect(jsonPath("$.policyPassed").value(false));

        mvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Facilities","amount":1200,"currency":"EUR",
                                 "quotationCount":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("BLOCKED"))
                .andExpect(jsonPath("$.requiresApproval").value(true))
                .andExpect(jsonPath("$.policyPassed").value(false));

        mvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Facilities","amount":300,"currency":"USD",
                                 "quotationCount":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("APPROVAL_REQUIRED"))
                .andExpect(jsonPath("$.requiresApproval").value(true));

        mvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Facilities","amount":300,"currency":"INR",
                                 "quotationCount":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("POLICY_PASSED"))
                .andExpect(jsonPath("$.policyPassed").value(true));

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE organization_id = ? AND topic = 'procurax.policy.v1'
                """, Integer.class, organization.getId())).isEqualTo(7);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events
                WHERE organization_id = ? AND action LIKE 'POLICY_%'
                """, Integer.class, organization.getId())).isEqualTo(7);
    }

    @Test
    void policiesFailClosedWithoutRulesAndCannotBeReadAcrossOrganizations() throws Exception {
        mvc.perform(get("/api/v1/policies/rules").with(authentication(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Facilities","amount":100,"currency":"INR",
                                 "quotationCount":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("APPROVAL_REQUIRED"))
                .andExpect(jsonPath("$.policyPassed").value(false));

        MvcResult created = mvc.perform(post("/api/v1/policies/rules")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Limit","category":"Facilities",
                                 "ruleType":"MAX_PURCHASE_AMOUNT",
                                 "thresholdAmount":1000,"thresholdCurrency":"INR","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String ruleId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();
        Organization foreignOrganization = organizationRepository.save(new Organization(
                "Policy Foreign Org", "policy-foreign-" + UUID.randomUUID()));
        User foreignUser = userRepository.save(new User(
                "policy-foreign-" + UUID.randomUUID() + "@example.com", "Foreign User", "google",
                UUID.randomUUID().toString()));
        Role foreignAdminRole = roleRepository.findByName("ORG_ADMIN").orElseThrow();
        memberRepository.save(new OrganizationMember(foreignOrganization.getId(), foreignUser.getId(),
                foreignAdminRole.getId()));
        OAuth2AuthenticationToken foreignToken = token(foreignUser, foreignOrganization, "ORG_ADMIN",
                permissionRepository.findPermissionNamesByRoleId(foreignAdminRole.getId()).toArray(String[]::new));

        mvc.perform(get("/api/v1/policies/rules").with(authentication(foreignToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Software","amount":100,"currency":"INR",
                                 "quotationCount":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("APPROVAL_REQUIRED"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/policies/rules/{id}", ruleId)
                        .with(authentication(foreignToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Takeover","ruleType":"MAX_PURCHASE_AMOUNT",
                                 "thresholdAmount":1,"thresholdCurrency":"INR","enabled":true}
                                """))
                .andExpect(status().isNotFound());

        Role buyerRole = roleRepository.findByName("BUYER").orElseThrow();
        OAuth2AuthenticationToken buyerOnlyToken = token(buyer, organization, "BUYER",
                permissionRepository.findPermissionNamesByRoleId(buyerRole.getId()).toArray(String[]::new));
        mvc.perform(post("/api/v1/policies/rules")
                        .with(authentication(buyerOnlyToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Unauthorized","ruleType":"MAX_PURCHASE_AMOUNT",
                                 "thresholdAmount":100,"thresholdCurrency":"INR","enabled":true}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvalRequestsRequirePolicyReviewAndAssignedApproverDecision() throws Exception {
        User approver = addMember("approver", "APPROVER");
        OAuth2AuthenticationToken approverToken = tokenForRole(approver, "APPROVER");
        QuoteFixture quote = acceptedQuotation("Facilities", new BigDecimal("2500.00"), "INR");

        MvcResult created = mvc.perform(post("/api/v1/approvals")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Emergency equipment purchase","category":"Facilities",
                                 "quotationId":"%s",
                                 "amount":2500,"currency":"INR","quotationCount":1,
                                 "justification":"Urgent replacement required","approverUserId":"%s"}
                                """.formatted(quote.quotationId(), approver.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requesterUserId").value(buyer.getId().toString()))
                .andExpect(jsonPath("$.steps[0].approverUserId").value(approver.getId().toString()))
                .andExpect(jsonPath("$.policyDecisions[0].outcome").value("APPROVAL_REQUIRED"))
                .andReturn();
        UUID approvalId = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText());

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM approval_requests WHERE id = ? AND organization_id = ?
                """, Integer.class, approvalId, organization.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_id = ? AND topic IN ('procurax.policy.v1', 'procurax.approval.v1')
                """, Integer.class, approvalId)).isEqualTo(1);

        mvc.perform(post("/api/v1/purchase-orders")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approvalRequestId":"%s","quotationId":"%s"}
                                """.formatted(approvalId, quote.quotationId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPROVAL_NOT_GRANTED"));

        mvc.perform(post("/api/v1/approvals/{id}/decision", approvalId)
                        .with(authentication(approverToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVE","comment":"Reviewed and approved"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.steps[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.steps[0].decisionComment").value("Reviewed and approved"));

        MvcResult purchaseOrder = mvc.perform(post("/api/v1/purchase-orders")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approvalRequestId":"%s","quotationId":"%s"}
                                """.formatted(approvalId, quote.quotationId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.totalAmount").value(2500))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].lineTotal").value(2500))
                .andReturn();
        UUID purchaseOrderId = UUID.fromString(objectMapper.readTree(
                purchaseOrder.getResponse().getContentAsString()).get("id").asText());
        String purchaseOrderLineId = objectMapper.readTree(
                purchaseOrder.getResponse().getContentAsString()).get("items").get(0).get("id").asText();
        Role buyerRole = roleRepository.findByName("BUYER").orElseThrow();
        OAuth2AuthenticationToken buyerOnlyToken = token(buyer, organization, "BUYER",
                permissionRepository.findPermissionNamesByRoleId(buyerRole.getId()).toArray(String[]::new));
        mvc.perform(post("/api/v1/payment-mandates")
                        .with(authentication(buyerOnlyToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchaseOrderId":"%s","expiresAt":"%s"}
                                """.formatted(purchaseOrderId, Instant.now().plusSeconds(3600))))
                .andExpect(status().isForbidden());

        User finance = addMember("finance", "FINANCE");
        OAuth2AuthenticationToken financeToken = tokenForRole(finance, "FINANCE");
        mvc.perform(post("/api/v1/shipments")
                        .with(authentication(buyerOnlyToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchaseOrderId":"%s","trackingNumber":"unauthorized-%s",
                                 "carrier":"Sandbox Freight",
                                 "items":[{"purchaseOrderItemId":"%s","quantity":1}]}
                                """.formatted(purchaseOrderId, UUID.randomUUID(), purchaseOrderLineId)))
                .andExpect(status().isForbidden());
        MvcResult shipmentCreated = mvc.perform(post("/api/v1/shipments")
                        .with(authentication(financeToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchaseOrderId":"%s","trackingNumber":"track-%s",
                                 "carrier":"Sandbox Freight","expectedAt":"%s",
                                 "items":[{"purchaseOrderItemId":"%s","quantity":1}]}
                                """.formatted(purchaseOrderId, UUID.randomUUID(),
                                        Instant.now().plusSeconds(3600), purchaseOrderLineId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andReturn();
        UUID shipmentId = UUID.fromString(objectMapper.readTree(
                shipmentCreated.getResponse().getContentAsString()).get("id").asText());
        mvc.perform(post("/api/v1/shipments")
                        .with(authentication(financeToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchaseOrderId":"%s","trackingNumber":"over-%s",
                                 "carrier":"Sandbox Freight",
                                 "items":[{"purchaseOrderItemId":"%s","quantity":1}]}
                                """.formatted(purchaseOrderId, UUID.randomUUID(), purchaseOrderLineId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHIPMENT_QUANTITY_EXCEEDED"));
        MvcResult invoiceCreated = mvc.perform(post("/api/v1/invoices")
                        .with(authentication(financeToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchaseOrderId":"%s","invoiceNumber":"INV-%s",
                                 "invoiceDate":"%s","dueDate":"%s","amount":2500,"currency":"INR"}
                                """.formatted(purchaseOrderId, UUID.randomUUID(),
                                        LocalDate.now(), LocalDate.now().plusDays(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECORDED"))
                .andReturn();
        UUID invoiceId = UUID.fromString(objectMapper.readTree(
                invoiceCreated.getResponse().getContentAsString()).get("id").asText());
        mvc.perform(post("/api/v1/reconciliations")
                        .with(authentication(financeToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"invoiceId":"%s"}
                                """.formatted(invoiceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCEPTION"))
                .andExpect(jsonPath("$.findings[0].code").value("FULFILLMENT_INCOMPLETE"));
        mvc.perform(post("/api/v1/shipments/{id}/status", shipmentId)
                        .with(authentication(financeToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DELIVERED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
        mvc.perform(post("/api/v1/shipments/{id}/status", shipmentId)
                        .with(authentication(financeToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"EXCEPTION","exceptionReason":"Late delivery"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHIPMENT_NOT_IN_TRANSIT"));
        MvcResult mandateResult = mvc.perform(post("/api/v1/payment-mandates")
                        .with(authentication(financeToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchaseOrderId":"%s","expiresAt":"%s"}
                                """.formatted(purchaseOrderId, Instant.now().plusSeconds(3600))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.maximumAmount").value(2500))
                .andReturn();
        UUID mandateId = UUID.fromString(objectMapper.readTree(
                mandateResult.getResponse().getContentAsString()).get("id").asText());
        String authorizeKey = "payment-authorize-" + UUID.randomUUID();
        mvc.perform(post("/api/v1/payments/authorize")
                        .with(authentication(buyerOnlyToken)).with(csrf())
                        .header("Idempotency-Key", "buyer-payment-auth-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchaseOrderId":"%s","mandateId":"%s"}
                                """.formatted(purchaseOrderId, mandateId)))
                .andExpect(status().isForbidden());
        MvcResult authorization = mvc.perform(post("/api/v1/payments/authorize")
                        .with(authentication(financeToken)).with(csrf())
                        .header("Idempotency-Key", authorizeKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchaseOrderId":"%s","mandateId":"%s"}
                                """.formatted(purchaseOrderId, mandateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.amount").value(2500))
                .andExpect(jsonPath("$.receipts.length()").value(1))
                .andExpect(jsonPath("$.receipts[0].operation").value("AUTHORIZED"))
                .andReturn();
        UUID paymentId = UUID.fromString(objectMapper.readTree(
                authorization.getResponse().getContentAsString()).get("id").asText());
        mvc.perform(post("/api/v1/payments/authorize")
                        .with(authentication(financeToken)).with(csrf())
                        .header("Idempotency-Key", authorizeKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchaseOrderId":"%s","mandateId":"%s"}
                                """.formatted(purchaseOrderId, mandateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()));
        String captureKey = "payment-capture-" + UUID.randomUUID();
        mvc.perform(post("/api/v1/payments/{id}/capture", paymentId)
                        .with(authentication(financeToken)).with(csrf())
                        .header("Idempotency-Key", captureKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CAPTURED"))
                .andExpect(jsonPath("$.receipts.length()").value(2));
        MvcResult matchedReconciliation = mvc.perform(post("/api/v1/reconciliations")
                        .with(authentication(financeToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"invoiceId":"%s"}
                                """.formatted(invoiceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCHED"))
                .andExpect(jsonPath("$.findings").isEmpty())
                .andReturn();
        UUID reconciliationId = UUID.fromString(objectMapper.readTree(
                matchedReconciliation.getResponse().getContentAsString()).get("id").asText());
        mvc.perform(post("/api/v1/payments/{id}/capture", paymentId)
                        .with(authentication(financeToken)).with(csrf())
                        .header("Idempotency-Key", captureKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receipts.length()").value(2));
        mvc.perform(post("/api/v1/payments/{id}/refund", paymentId)
                        .with(authentication(financeToken)).with(csrf())
                        .header("Idempotency-Key", "payment-refund-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.receipts.length()").value(3));
        mvc.perform(post("/api/v1/reconciliations")
                        .with(authentication(financeToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"invoiceId":"%s"}
                                """.formatted(invoiceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCEPTION"))
                .andExpect(jsonPath("$.findings[0].code").value("PAYMENT_NOT_SETTLED"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM payment_receipts
                WHERE payment_intent_id = ? AND organization_id = ?
                """, Integer.class, paymentId, organization.getId())).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events
                WHERE resource_type = 'PAYMENT' AND resource_id = ?
                """, Integer.class, paymentId.toString())).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_id = ? AND topic = 'procurax.payment.v1'
                """, Integer.class, paymentId)).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE organization_id = ? AND topic = 'procurax.fulfillment.v1'
                """, Integer.class, organization.getId())).isEqualTo(6);
        mvc.perform(post("/api/v1/purchase-orders")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approvalRequestId":"%s","quotationId":"%s"}
                                """.formatted(approvalId, quote.quotationId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PURCHASE_ORDER_ALREADY_EXISTS"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM purchase_orders
                WHERE id = ? AND organization_id = ? AND approval_request_id = ?
                """, Integer.class, purchaseOrderId, organization.getId(), approvalId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events
                WHERE resource_type = 'PURCHASE_ORDER' AND resource_id = ?
                """, Integer.class, purchaseOrderId.toString())).isEqualTo(1);
        Organization foreignOrganization = organizationRepository.save(new Organization(
                "PO Foreign Org", "po-foreign-" + UUID.randomUUID()));
        User foreignAdmin = userRepository.save(new User(
                "po-foreign-" + UUID.randomUUID() + "@example.com", "Foreign Admin", "google",
                UUID.randomUUID().toString()));
        Role adminRole = roleRepository.findByName("ORG_ADMIN").orElseThrow();
        memberRepository.save(new OrganizationMember(foreignOrganization.getId(), foreignAdmin.getId(),
                adminRole.getId()));
        OAuth2AuthenticationToken foreignToken = token(foreignAdmin, foreignOrganization, "ORG_ADMIN",
                permissionRepository.findPermissionNamesByRoleId(adminRole.getId()).toArray(String[]::new));
        mvc.perform(get("/api/v1/purchase-orders/{id}", purchaseOrderId).with(authentication(foreignToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/payments/{id}", paymentId).with(authentication(foreignToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/shipments/{id}", shipmentId).with(authentication(foreignToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/invoices/{id}", invoiceId).with(authentication(foreignToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/reconciliations/{id}", reconciliationId)
                        .with(authentication(foreignToken)))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/approvals/{id}/decision", approvalId)
                        .with(authentication(approverToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPROVAL_ALREADY_DECIDED"));
        mvc.perform(post("/api/v1/approvals")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Self approval attempt","category":"Facilities",
                                 "quotationId":"%s",
                                 "amount":2500,"currency":"INR","quotationCount":1,
                                 "justification":"Should be rejected","approverUserId":"%s"}
                                """.formatted(quote.quotationId(), buyer.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SELF_APPROVAL_NOT_ALLOWED"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events
                WHERE resource_type = 'APPROVAL_REQUEST' AND resource_id = ?
                """, Integer.class, approvalId.toString())).isEqualTo(2);
    }

    @Test
    void blockedPurchasesCannotCreateApprovalRequestsButPolicyDecisionIsAudited() throws Exception {
        User approver = addMember("gate-approver", "APPROVER");
        QuoteFixture quote = acceptedQuotation("Facilities", new BigDecimal("500.00"), "INR");
        mvc.perform(post("/api/v1/policies/rules")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"USD only","ruleType":"ALLOWED_CURRENCIES",
                                 "allowedCurrencyCodes":["USD"],"enabled":true}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/approvals")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Disallowed currency purchase","category":"Facilities",
                                 "quotationId":"%s",
                                 "amount":500,"currency":"INR","quotationCount":1,
                                 "justification":"Try to escalate a blocked purchase",
                                 "approverUserId":"%s"}
                                """.formatted(quote.quotationId(), approver.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PURCHASE_BLOCKED_BY_POLICY"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM approval_requests WHERE organization_id = ?
                """, Integer.class, organization.getId())).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE organization_id = ? AND event_type = 'POLICY_EVALUATED'
                """, Integer.class, organization.getId())).isEqualTo(1);
    }

    @Test
    void approvalRejectionsRequireReasonsAndRequestsAreTenantScoped() throws Exception {
        User assignedApprover = addMember("assigned-approver", "APPROVER");
        OAuth2AuthenticationToken assignedToken = tokenForRole(assignedApprover, "APPROVER");
        User otherApprover = addMember("other-approver", "APPROVER");
        OAuth2AuthenticationToken otherToken = tokenForRole(otherApprover, "APPROVER");
        QuoteFixture quote = acceptedQuotation("Software", new BigDecimal("500.00"), "INR");

        MvcResult created = mvc.perform(post("/api/v1/approvals")
                        .with(authentication(buyerToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Software purchase","category":"Software",
                                 "quotationId":"%s",
                                 "amount":500,"currency":"INR","quotationCount":1,
                                 "justification":"Policy exception requested","approverUserId":"%s"}
                                """.formatted(quote.quotationId(), assignedApprover.getId())))
                .andExpect(status().isOk())
                .andReturn();
        UUID approvalId = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/api/v1/approvals/{id}/decision", approvalId)
                        .with(authentication(otherToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"REJECT","comment":"Not assigned"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPROVAL_NOT_ASSIGNED"));
        mvc.perform(post("/api/v1/approvals/{id}/decision", approvalId)
                        .with(authentication(assignedToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"REJECT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REJECTION_COMMENT_REQUIRED"));
        mvc.perform(post("/api/v1/approvals/{id}/decision", approvalId)
                        .with(authentication(assignedToken)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"REJECT","comment":"Insufficient supporting quotes"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        Organization foreignOrganization = organizationRepository.save(new Organization(
                "Approval Foreign Org", "approval-foreign-" + UUID.randomUUID()));
        User foreignAdmin = userRepository.save(new User(
                "approval-foreign-" + UUID.randomUUID() + "@example.com", "Foreign Admin", "google",
                UUID.randomUUID().toString()));
        Role adminRole = roleRepository.findByName("ORG_ADMIN").orElseThrow();
        memberRepository.save(new OrganizationMember(foreignOrganization.getId(), foreignAdmin.getId(),
                adminRole.getId()));
        OAuth2AuthenticationToken foreignToken = token(foreignAdmin, foreignOrganization, "ORG_ADMIN",
                permissionRepository.findPermissionNamesByRoleId(adminRole.getId()).toArray(String[]::new));
        mvc.perform(get("/api/v1/approvals/{id}", approvalId).with(authentication(foreignToken)))
                .andExpect(status().isNotFound());
    }

    private User addMember(String prefix, String roleName) {
        User user = userRepository.save(new User(prefix + "-" + UUID.randomUUID() + "@example.com",
                prefix, "google", UUID.randomUUID().toString()));
        Role role = roleRepository.findByName(roleName).orElseThrow();
        memberRepository.save(new OrganizationMember(organization.getId(), user.getId(), role.getId()));
        return user;
    }

    private QuoteFixture acceptedQuotation(String category, BigDecimal amount, String currency) {
        Rfq rfq = new Rfq(organization.getId(), "Purchase " + category, "Approved purchase test",
                "RFQ", category, amount.add(new BigDecimal("1000.00")), currency,
                Instant.now().plusSeconds(3600), 14, UUID.randomUUID());
        rfq.publish();
        rfq.markInProgress();
        Rfq savedRfq = rfqRepository.save(rfq);
        RfqItem rfqItem = rfqItemRepository.save(new RfqItem(organization.getId(), savedRfq.getId(),
                "Test item", 1, "each", "Test specification"));
        Vendor vendor = vendorRepository.save(new Vendor(organization.getId(),
                "Approval Vendor " + UUID.randomUUID(), "vendor@example.test", category, 30));
        Quotation quotation = new Quotation(organization.getId(), savedRfq.getId(), vendor.getId(),
                amount, currency, 14, 30, null, objectMapper.createObjectNode(), UUID.randomUUID());
        quotation.updateStatus("ACCEPTED");
        Quotation savedQuotation = quotationRepository.save(quotation);
        quotationItemRepository.save(new QuotationItem(organization.getId(), savedQuotation.getId(),
                rfqItem.getId(), amount, 1));
        return new QuoteFixture(savedQuotation.getId(), savedRfq.getId(), vendor.getId());
    }

    private record QuoteFixture(UUID quotationId, UUID rfqId, UUID vendorId) {
    }

    private OAuth2AuthenticationToken tokenForRole(User user, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        return token(user, organization, roleName,
                permissionRepository.findPermissionNamesByRoleId(role.getId()).toArray(String[]::new));
    }

    private OAuth2AuthenticationToken token(User user, Organization org, String role, String... permissions) {
        SecurityPrincipal principal = new SecurityPrincipal(user.getId(), user.getEmail(), user.getFullName(),
                org.getId(), org.getName(), role, Set.of(permissions), Map.of("sub", user.getProviderSubject()));
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }
}

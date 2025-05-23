package com.userapi.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.userapi.models.external.EmploymentInfo;
import com.userapi.models.internal.EmploymentInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmploymentInfoConverterTest {

    private final EmploymentInfoConverter converter = new EmploymentInfoConverter();
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void doForward_nullInput_returnsNull() {
        assertNull(converter.doForward(null));
    }

    @Test
    void doForward_validInput_convertsCorrectly() {
        EmploymentInfo external = new EmploymentInfo();
        external.setJobTitle("Developer");
        external.setOrganizationUnit("Engineering");
        external.setStartDate(LocalDate.of(2020, 1, 1).atStartOfDay());
        external.setEndDate(LocalDate.of(2023, 1, 1).atStartOfDay());
        external.setReportingManager("Manager Name");
        external.setExtensionsData(Map.of("key1", "value1"));

        EmploymentInfoDto dto = converter.doForward(external);

        assertNotNull(dto);
        assertEquals("Developer", dto.getJobTitle());
        assertEquals("Engineering", dto.getOrganizationUnit());
        assertEquals(LocalDate.of(2020, 1, 1).atStartOfDay(), dto.getStartDate());
        assertEquals(LocalDate.of(2023, 1, 1).atStartOfDay(), dto.getEndDate());
        assertEquals("Manager Name", dto.getReportingManager());
        assertEquals(Map.of("key1", "value1"), dto.getExtensionsData());
    }

    @Test
    void doBackward_nullInput_returnsNull() {
        assertNull(converter.doBackward(null));
    }

    @Test
    void doBackward_validInput_convertsCorrectly() {
        EmploymentInfoDto dto = EmploymentInfoDto.builder()
                .jobTitle("Tester")
                .organizationUnit("QA")
                .startDate(LocalDate.of(2019, 6, 1).atStartOfDay())
                .endDate(LocalDate.of(2022, 6, 1).atStartOfDay())
                .reportingManager("QA Manager")
                .extensionsData(Map.of("extra", "data"))
                .build();

        EmploymentInfo external = converter.doBackward(dto);

        assertNotNull(external);
        assertEquals("Tester", external.getJobTitle());
        assertEquals("QA", external.getOrganizationUnit());
        assertEquals(LocalDate.of(2019, 6, 1).atStartOfDay(), external.getStartDate());
        assertEquals(LocalDate.of(2022, 6, 1).atStartOfDay(), external.getEndDate());
        assertEquals("QA Manager", external.getReportingManager());
        assertEquals(Map.of("extra", "data"), external.getExtensionsData());
    }

    @Test
    void deserialize_invalidDateFormat_throwsException() {
        String json = "{\n" +
                "  \"jobTitle\": \"Developer\",\n" +
                "  \"organizationUnit\": \"Engineering\",\n" +
                "  \"startDate\": \"invalid-date-format\",\n" +
                "  \"reportingManager\": \"Manager Name\",\n" +
                "  \"extensionsData\": {\"key1\": \"value1\"}\n" +
                "}";

        assertThrows(InvalidFormatException.class, () -> {
            objectMapper.readValue(json, EmploymentInfo.class);
        });
    }

    @Test
    void deserialize_missingRequiredDate_throwsException() {
        String json = "{\n" +
                "  \"jobTitle\": \"Developer\",\n" +
                "  \"organizationUnit\": \"Engineering\",\n" +
                "  \"reportingManager\": \"Manager Name\",\n" +
                "  \"extensionsData\": {\"key1\": \"value1\"}\n" +
                "}";

        // This should throw an exception because startDate is required
        assertThrows(Exception.class, () -> {
            EmploymentInfo employmentInfo = objectMapper.readValue(json, EmploymentInfo.class);
            // The validation happens when we try to convert to DTO
            converter.doForward(employmentInfo);
        });
    }

    @Test
    void deserialize_outOfRangeDate_throwsException() {
        String json = "{\n" +
                "  \"jobTitle\": \"Developer\",\n" +
                "  \"organizationUnit\": \"Engineering\",\n" +
                "  \"startDate\": \"9999-99-99T00:00:00\",\n" +
                "  \"reportingManager\": \"Manager Name\",\n" +
                "  \"extensionsData\": {\"key1\": \"value1\"}\n" +
                "}";

        // Jackson wraps DateTimeParseException in InvalidFormatException
        assertThrows(InvalidFormatException.class, () -> {
            objectMapper.readValue(json, EmploymentInfo.class);
        });
    }

    @Test
    void deserialize_validDateFormat_parsesCorrectly() throws JsonProcessingException {
        String json = "{\n" +
                "  \"jobTitle\": \"Developer\",\n" +
                "  \"organizationUnit\": \"Engineering\",\n" +
                "  \"startDate\": \"2020-01-01T00:00:00\",\n" +
                "  \"reportingManager\": \"Manager Name\",\n" +
                "  \"extensionsData\": {\"key1\": \"value1\"}\n" +
                "}";

        EmploymentInfo employmentInfo = objectMapper.readValue(json, EmploymentInfo.class);

        assertNotNull(employmentInfo);
        assertEquals("Developer", employmentInfo.getJobTitle());
        assertEquals("Engineering", employmentInfo.getOrganizationUnit());
        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0, 0), employmentInfo.getStartDate());
        assertEquals("Manager Name", employmentInfo.getReportingManager());
    }

    @Test
    void deserialize_nullDate_parsesCorrectly() throws JsonProcessingException {
        String json = "{\n" +
                "  \"jobTitle\": \"Developer\",\n" +
                "  \"organizationUnit\": \"Engineering\",\n" +
                "  \"startDate\": \"2020-01-01T00:00:00\",\n" +
                "  \"endDate\": null,\n" +
                "  \"reportingManager\": \"Manager Name\",\n" +
                "  \"extensionsData\": {\"key1\": \"value1\"}\n" +
                "}";

        EmploymentInfo employmentInfo = objectMapper.readValue(json, EmploymentInfo.class);

        assertNotNull(employmentInfo);
        assertEquals("Developer", employmentInfo.getJobTitle());
        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0, 0), employmentInfo.getStartDate());
        assertNull(employmentInfo.getEndDate());
    }

    @Test
    void deserialize_differentDateFormats_throwsException() {
        String json = "{\n" +
                "  \"jobTitle\": \"Developer\",\n" +
                "  \"organizationUnit\": \"Engineering\",\n" +
                "  \"startDate\": \"2020/01/01\",\n" +
                "  \"reportingManager\": \"Manager Name\",\n" +
                "  \"extensionsData\": {\"key1\": \"value1\"}\n" +
                "}";

        assertThrows(InvalidFormatException.class, () -> {
            objectMapper.readValue(json, EmploymentInfo.class);
        });
    }

    @Test
    void doForward_withNullDates_convertsCorrectly() {
        EmploymentInfo external = new EmploymentInfo();
        external.setJobTitle("Developer");
        external.setOrganizationUnit("Engineering");
        external.setStartDate(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        external.setEndDate(null); // Explicitly set to null
        external.setReportingManager("Manager");
        external.setExtensionsData(Map.of("key1", "value1")); // Required field

        EmploymentInfoDto dto = converter.doForward(external);

        assertNotNull(dto);
        assertEquals("Developer", dto.getJobTitle());
        assertEquals("Engineering", dto.getOrganizationUnit());
        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0, 0), dto.getStartDate());
        assertNull(dto.getEndDate());
        assertEquals("Manager", dto.getReportingManager());
        assertEquals(Map.of("key1", "value1"), dto.getExtensionsData());
    }
}

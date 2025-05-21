package com.userapi.converters;

import com.userapi.models.external.EmploymentInfo;
import com.userapi.models.internal.EmploymentInfoDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmploymentInfoConverterTest {

    private final EmploymentInfoConverter converter = new EmploymentInfoConverter();

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
}

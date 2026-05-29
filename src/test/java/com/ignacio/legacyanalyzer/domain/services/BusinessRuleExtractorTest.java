package com.ignacio.legacyanalyzer.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.domain.model.BusinessRuleMetadata;

public class BusinessRuleExtractorTest {



  @Test
  void shouldDetectBusinessRules() {

    String sql = """
        BEGIN

            RAISE_APPLICATION_ERROR(
                -20001,
                'Saldo insuficiente'
            );

            RAISE_APPLICATION_ERROR(
                -20002,
                'Cliente bloqueado'
            );

        END;
        """;

    BusinessRuleExtractor extractor = new BusinessRuleExtractor();

    List<BusinessRuleMetadata> result = extractor.extract(sql);

    assertEquals(2, result.size());

    assertTrue(result.stream().anyMatch(r -> r.errorCode().equals("-20001")));

    assertTrue(result.stream().anyMatch(r -> r.errorCode().equals("-20002")));
  }

}

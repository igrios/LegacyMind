package com.ignacio.legacyanalyzer.domain.services.semantic;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ignacio.legacyanalyzer.domain.model.BusinessRuleMetadata;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BusinessRuleExtractor {

  
private static final Pattern RAISE_PATTERN =
    Pattern.compile(
        "RAISE_APPLICATION_ERROR\\s*\\(\\s*(-?\\d+)\\s*,\\s*'([^']*)'",
        Pattern.CASE_INSENSITIVE);

    public List<BusinessRuleMetadata> extract(String sql) {

        log.debug("Extracting business rules from source: {}", sql);
        List<BusinessRuleMetadata> rules =
                new ArrayList<>();

        Matcher matcher =
                RAISE_PATTERN.matcher(sql);

        while (matcher.find()) {

            String errorCode =
                    matcher.group(1);

            String message =
                    matcher.group(2);

            rules.add(

                    new BusinessRuleMetadata(

                            errorCode,

                            message));
        }

        return rules;
    }
}

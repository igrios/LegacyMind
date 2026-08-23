package com.ignacio.legacyanalyzer.domain.services.semantic;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ignacio.legacyanalyzer.domain.model.DbLinkMetadata;
import org.springframework.stereotype.Service;

// Servicio para extraer referencias a DB Links en el código SQL
@Service
public class DbLinkSemanticExtractor {

    private static final Pattern DBLINK_PATTERN =
            Pattern.compile(
                    "([A-Z][A-Z0-9_.$#]*)@([A-Z][A-Z0-9_.$#]+)",
                    Pattern.CASE_INSENSITIVE);

    public List<DbLinkMetadata> extract(String sql) {

        List<DbLinkMetadata> result = new ArrayList<>();

        Matcher matcher = DBLINK_PATTERN.matcher(sql);

        while (matcher.find()) {

            result.add(
                    new DbLinkMetadata(
                            matcher.group(1).toUpperCase(),
                            matcher.group(2).toUpperCase()));
        }

        return result;
    }
}

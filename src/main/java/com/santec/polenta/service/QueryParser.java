package com.santec.polenta.service;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.HashMap;

@Component
public class QueryParser {
    private static final Map<Pattern, String> QUERY_PATTERNS;
    static {
        Map<Pattern, String> patterns = new HashMap<>();
        patterns.put(Pattern.compile("(?i).*show.*tables.*"), "SHOW_TABLES");
        patterns.put(Pattern.compile("(?i).*list.*tables.*"), "SHOW_TABLES");
        patterns.put(Pattern.compile("(?i).*what.*tables.*"), "SHOW_TABLES");
        patterns.put(Pattern.compile("(?i).*accessible.*tables.*"), "ACCESSIBLE_TABLES");
        patterns.put(Pattern.compile("(?i).*tables.*can.*access.*"), "ACCESSIBLE_TABLES");
        patterns.put(Pattern.compile("(?i).*describe.*table.*"), "DESCRIBE_TABLE");
        patterns.put(Pattern.compile("(?i).*columns.*in.*"), "DESCRIBE_TABLE");
        patterns.put(Pattern.compile("(?i).*structure.*of.*"), "DESCRIBE_TABLE");
        patterns.put(Pattern.compile("(?i).*sample.*data.*from.*"), "SAMPLE_DATA");
        patterns.put(Pattern.compile("(?i).*show.*data.*from.*"), "SAMPLE_DATA");
        patterns.put(Pattern.compile("(?i).*preview.*"), "SAMPLE_DATA");
        patterns.put(Pattern.compile("(?i).*lista de [a-zA-Záéíóúñ]+.*"), "LIST_ENTITY");
        QUERY_PATTERNS = java.util.Collections.unmodifiableMap(patterns);
    }

    public String identifyQueryType(String query) {
        for (Map.Entry<Pattern, String> entry : QUERY_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(query).matches()) {
                return entry.getValue();
            }
        }
        if (query.toLowerCase().contains("find") || query.toLowerCase().contains("search")) {
            return "SEARCH_TABLES";
        }
        if (query.toLowerCase().trim().startsWith("select") ||
                query.toLowerCase().trim().startsWith("show") ||
                query.toLowerCase().trim().startsWith("describe")) {
            return "DIRECT_SQL";
        }
        return "UNKNOWN";
    }

    public String extractTableName(String query, TokenizerService tokenizerService) {
        String[] words = query.toLowerCase().split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].equals("from") || words[i].equals("table") || words[i].equals("of")) {
                return words[i + 1].replaceAll("[^a-zA-Z0-9._]", "");
            }
        }
        String lastWord = words[words.length - 1].replaceAll("[^a-zA-Z0-9._]", "");
        if (lastWord.length() > 0) {
            return lastWord;
        }
        return null;
    }

    public String extractEntityFromQuery(String query, TokenizerService tokenizerService) {
        String[] tokens = tokenizerService.tokenize(query);
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("lista")) {
                if (i + 2 < tokens.length && tokens[i + 1].equalsIgnoreCase("de")) {
                    return tokens[i + 2].toLowerCase();
                } else if (i + 1 < tokens.length) {
                    return tokens[i + 1].toLowerCase();
                }
            }
        }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("lista de ([a-zA-Záéíóúñ]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(query);
        if (m.find()) {
            String entity = m.group(1).toLowerCase();
            if (entity.endsWith("es")) entity = entity.substring(0, entity.length() - 2);
            else if (entity.endsWith("s")) entity = entity.substring(0, entity.length() - 1);
            return entity;
        }
        return null;
    }

    public String extractSearchKeyword(String query, TokenizerService tokenizerService) {
        String[] tokens = tokenizerService.tokenize(query);
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equalsIgnoreCase("find") || tokens[i].equalsIgnoreCase("search") || tokens[i].equalsIgnoreCase("for")) {
                return tokens[i + 1].replaceAll("[^a-zA-Z0-9._]", "");
            }
        }
        return null;
    }
}

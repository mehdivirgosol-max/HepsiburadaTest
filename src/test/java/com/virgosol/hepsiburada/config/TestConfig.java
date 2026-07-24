package com.virgosol.hepsiburada.config;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

public record TestConfig(
        URI baseUri,
        String email,
        String password,
        boolean headless,
        Duration timeout,
        Duration typingDelay
) {
    private static final String DEFAULT_BASE_URL = "https://www.hepsiburada.com";

    public static TestConfig fromEnvironment() {
        String email = requireSecret("HB_EMAIL");
        String password = requireSecret("HB_PASSWORD");
        URI baseUri = parseBaseUri(optional("HB_BASE_URL", DEFAULT_BASE_URL));
        boolean headless = parseBoolean("HB_HEADLESS", false);
        int timeoutSeconds = parseBoundedInteger("HB_TIMEOUT_SECONDS", 25, 5, 120);
        int typingDelayMillis = parseBoundedInteger("HB_TYPING_DELAY_MS", 60, 0, 500);

        if (!email.contains("@")) {
            throw new IllegalStateException("HB_EMAIL geçerli bir e-posta adresi olmalıdır.");
        }

        return new TestConfig(
                baseUri,
                email,
                password,
                headless,
                Duration.ofSeconds(timeoutSeconds),
                Duration.ofMillis(typingDelayMillis)
        );
    }

    public void assertTrustedUrl(String url, String operation) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new SecurityException(operation + " için geçerli sayfa adresi alınamadı.", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !isTrustedHost(uri.getHost())) {
            throw new SecurityException(
                    operation + " yalnızca güvenilir bir Hepsiburada HTTPS alan adında yapılabilir."
            );
        }
    }

    @Override
    public String toString() {
        return "TestConfig[baseUri=%s, email=[REDACTED], password=[REDACTED], "
                .formatted(baseUri)
                + "headless=%s, timeout=%s, typingDelay=%s]"
                .formatted(headless, timeout, typingDelay);
    }

    private static String requireSecret(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    name + " ortam değişkeni tanımlı ve boş olmayan bir değer içermelidir."
            );
        }
        return value;
    }

    private static String optional(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static URI parseBaseUri(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("HB_BASE_URL geçerli bir URI olmalıdır.", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalStateException("HB_BASE_URL mutlak bir HTTPS adresi olmalıdır.");
        }
        if (!isTrustedHost(uri.getHost())) {
            throw new IllegalStateException(
                    "HB_BASE_URL hepsiburada.com veya güvenilir bir alt alan adı olmalıdır."
            );
        }
        return uri;
    }

    private static boolean isTrustedHost(String host) {
        if (host == null) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return normalizedHost.equals("hepsiburada.com")
                || normalizedHost.endsWith(".hepsiburada.com");
    }

    private static boolean parseBoolean(String name, boolean defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes" -> true;
            case "false", "0", "no" -> false;
            default -> throw new IllegalStateException(name + " true veya false olmalıdır.");
        };
    }

    private static int parseBoundedInteger(
            String name,
            int defaultValue,
            int minimum,
            int maximum
    ) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < minimum || parsed > maximum) {
                throw new IllegalStateException(
                        name + " " + minimum + " ile " + maximum + " arasında olmalıdır."
                );
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(name + " tam sayı olmalıdır.", exception);
        }
    }
}

package com.virgosol.hepsiburada.model;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SelectedProduct(String name, String url, String code) {
    private static final Pattern PRODUCT_CODE = Pattern.compile(
            "(?i)-p-([a-z0-9]+)(?:[/?#]|$)"
    );

    public SelectedProduct {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Seçilen ürünün adı boş olamaz.");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Seçilen ürünün URL adresi boş olamaz.");
        }
        code = code == null ? "" : code;
    }

    public static SelectedProduct from(String name, String url) {
        Matcher matcher = PRODUCT_CODE.matcher(url);
        String code = matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : "";
        return new SelectedProduct(name.trim(), url, code);
    }
}

package com.virgosol.hepsiburada.support;

import com.thoughtworks.gauge.Gauge;

public final class TestLog {
    private TestLog() {
    }

    public static void success(String message, Object... arguments) {
        write("[BAŞARILI] " + message.formatted(arguments));
    }

    public static void info(String message, Object... arguments) {
        write("[BİLGİ] " + message.formatted(arguments));
    }

    public static void warning(String message, Object... arguments) {
        String formatted = "[UYARI] " + message.formatted(arguments);
        System.err.println(formatted);
        Gauge.writeMessage(formatted);
    }

    private static void write(String message) {
        System.out.println(message);
        Gauge.writeMessage(message);
    }
}

package com.virgosol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class HepsiburadaTestLauncher {
    private HepsiburadaTestLauncher() {
    }

    public static void main(String[] args) {
        requireEnvironmentVariable("HB_EMAIL");
        requireEnvironmentVariable("HB_PASSWORD");

        Path projectRoot = findProjectRoot();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "cmd.exe",
                "/d",
                "/c",
                projectRoot.resolve("run-tests.cmd").toString()
        );
        processBuilder.directory(projectRoot.toFile());
        processBuilder.inheritIO();

        try {
            int exitCode = processBuilder.start().waitFor();
            if (exitCode != 0) {
                System.err.println(
                        "[HATA] Hepsiburada Gauge testi başarısız oldu. Çıkış kodu: " + exitCode
                );
            }
            System.exit(exitCode);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "run-tests.cmd başlatılamadı. Maven ve Gauge PATH üzerinde olmalıdır.",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test çalıştırması kesildi.", exception);
        }
    }

    private static Path findProjectRoot() {
        Path candidate = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("pom.xml"))
                    && Files.isRegularFile(candidate.resolve("run-tests.cmd"))
                    && Files.isDirectory(candidate.resolve("specs"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new IllegalStateException(
                "Proje kökü bulunamadı. IntelliJ çalışma dizini $PROJECT_DIR$ olmalıdır."
        );
    }

    private static void requireEnvironmentVariable(String name) {
        Map<String, String> environment = System.getenv();
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    name + " ortam değişkeni IntelliJ Run Configuration tarafından görülemiyor. "
                            + "Değişkeni tanımladıktan sonra IntelliJ IDEA'yı yeniden başlatın."
            );
        }
    }
}

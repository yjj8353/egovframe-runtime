package org.egovframe.rte.fdl.crypto.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EgovCryptoConfigReaderTest {

    @Test
    @DisplayName("기본 생성자 EgovCryptoConfigReader() 테스트")
    void testConfigPathAndResourceLoaderBothNull() {
        EgovCryptoConfigReader reader = new EgovCryptoConfigReader();
        EgovCryptoConfig config = reader.readConfig();

        // EgovCryptoConfigReader()인 경우, 모든 설정은 기본 설정을 따른다
        // 전제: 테스트 classpath의 기본 경로(egovframework/egovProps/conf/egov-crypto-config.properties)에
        // 설정 파일이 없어야 한다. 테스트 리소스는 egovframework/conf/ 아래에 있으므로 현재는 createDefaultConfig() 값이 반환된다.
        // 기본 경로에 테스트 리소스를 추가하면 이 테스트는 해당 파일의 값을 검증하게 되므로 기대값을 함께 수정해야 한다.
        assertEquals("SHA-256", config.getAlgorithm());
        assertEquals("egovframe", config.getAlgorithmKey());
        assertEquals("gdyYs/IZqY86VcWhT8emCYfqY1ahw2vtLG+/FzNqtrQ=", config.getAlgorithmKeyHash());
        assertEquals(1024, config.getCryptoBlockSize());
        assertEquals("classpath:/egovframework/egovProps/globals.properties", config.getCryptoPropertyLocation());
    }

    @Test
    @DisplayName("설정 파일에 생략된 키는 기본값 유지 테스트")
    void testReadConfigKeepsSafeDefaultsForOmittedKeys(@TempDir Path tempDir) throws IOException {
        // cryptoPropertyLocation만 지정하고 crypto/algorithm/cryptoBlockSize는 생략한 부분 설정 파일.
        Path propsFile = tempDir.resolve("egov-crypto-config.properties");
        Properties props = new Properties();
        props.setProperty("cryptoPropertyLocation", "classpath:/custom.properties");
        try (BufferedWriter writer = Files.newBufferedWriter(propsFile, StandardCharsets.UTF_8)) {
            props.store(writer, null);
        }

        EgovCryptoConfigReader reader = new EgovCryptoConfigReader("file:" + propsFile, null);
        EgovCryptoConfig config = reader.readConfig();

        assertEquals("classpath:/custom.properties", config.getCryptoPropertyLocation());
        // 형제 구현체 EgovAccessConfigReader·EgovSecurityConfigReader와 동일하게, 파일에 명시되지 않은
        // 키는 createDefaultConfig()의 기본값(crypto=true, algorithm=SHA-256, cryptoBlockSize=1024)을 유지해야 한다.
        assertTrue(config.isCrypto());
        assertEquals("SHA-256", config.getAlgorithm());
        assertEquals(1024, config.getCryptoBlockSize());
    }

    @Test
    @DisplayName("설정 파일에 명시한 값이 기본값보다 우선 적용 테스트")
    void testReadConfigHonorsExplicitOverrideOfDefaultValue(@TempDir Path tempDir) throws IOException {
        // 사용자가 crypto를 명시적으로 false로 끈 경우, 기본값(true)에 밀려서는 안 된다.
        Path propsFile = tempDir.resolve("egov-crypto-config.properties");
        Properties props = new Properties();
        props.setProperty("crypto", "false");
        try (BufferedWriter writer = Files.newBufferedWriter(propsFile, StandardCharsets.UTF_8)) {
            props.store(writer, null);
        }

        EgovCryptoConfigReader reader = new EgovCryptoConfigReader("file:" + propsFile, null);
        EgovCryptoConfig config = reader.readConfig();

        assertFalse(config.isCrypto());
        // 명시 안 한 다른 키는 여전히 기본값을 유지해야 한다.
        assertEquals("SHA-256", config.getAlgorithm());
    }

    @Test
    @DisplayName("튀르키예어 로케일에서 설정 키 반영 테스트")
    void testReadConfigAppliesKeysUnderTurkishLocale(@TempDir Path tempDir) throws IOException {
        // 터키어 로케일에서는 "id".substring(0, 1).toUpperCase()가 점 있는 'İ'(U+0130)가 되어
        // setter 이름이 "setİd"로 만들어지고, findSetter가 null을 돌려주면서 이 키가 조용히 무시된다.
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            Path propsFile = tempDir.resolve("egov-crypto-config.properties");
            Properties props = new Properties();
            props.setProperty("id", "customConfigId");
            try (BufferedWriter writer = Files.newBufferedWriter(propsFile, StandardCharsets.UTF_8)) {
                props.store(writer, null);
            }

            EgovCryptoConfigReader reader = new EgovCryptoConfigReader("file:" + propsFile, null);
            EgovCryptoConfig config = reader.readConfig();

            assertEquals("customConfigId", config.getId(),
                    "터키어 로케일에서도 properties의 id 키가 반영되어야 한다(기본값 egovCryptoConfig로 남으면 안 된다)");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @DisplayName("configPath 파일 경로를 file:/ 접두사 없이 호출")
    void testNonFilePrefix(@TempDir Path tempDir) throws Exception {
        Path propsFile = tempDir.resolve("egov-crypto-config.properties");
        Properties props = new Properties();
        props.setProperty("crypto", "false");
        try (BufferedWriter writer = Files.newBufferedWriter(propsFile, StandardCharsets.UTF_8)) {
            props.store(writer, null);
        }

        EgovCryptoConfigReader reader = new EgovCryptoConfigReader(propsFile.toString(), null);
        EgovCryptoConfig config = reader.readConfig();

        assertFalse(config.isCrypto());
        // 명시 안 한 다른 키는 여전히 기본값을 유지해야 한다.
        assertEquals("SHA-256", config.getAlgorithm());
    }

    @Test
    @DisplayName("문자열 값의 뒤쪽 공백 제거 테스트")
    void testReadConfigTrimsTrailingWhitespaceOfStringValue(@TempDir Path tempDir) throws IOException {
        // Properties는 값의 앞쪽 공백만 제거하므로, 뒤쪽 공백은 리더에서 제거해야 한다.
        // ("SHA-256 "이 그대로 들어가면 MessageDigest 알고리즘 조회가 실패한다)
        Path propsFile = tempDir.resolve("egov-crypto-config.properties");
        try (BufferedWriter writer = Files.newBufferedWriter(propsFile, StandardCharsets.UTF_8)) {
            writer.write("algorithm=SHA-512   \n");
            writer.write("algorithmKey=  customKey\t\n");
        }

        EgovCryptoConfigReader reader = new EgovCryptoConfigReader("file:" + propsFile, null);
        EgovCryptoConfig config = reader.readConfig();

        assertEquals("SHA-512", config.getAlgorithm());
        assertEquals("customKey", config.getAlgorithmKey());
    }
}

/*
 * Copyright 2008-2024 MOIS(Ministry of the Interior and Safety).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.egovframe.rte.fdl.crypto.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

/**
 * egov-crypto properties 설정 파일을 읽는 유틸리티 클래스
 *
 * <p>Desc.: properties 설정 파일을 읽어 EgovCryptoConfig 객체로 변환하는 클래스</p>
 *
 * @author 유지보수
 * @version 6.0
 * @since 2025.06.01
 */
public class EgovCryptoConfigReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(EgovCryptoConfigReader.class);
    private static final String DEFAULT_CONFIG_PATH = "egovframework/egovProps/conf/egov-crypto-config.properties";
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String FILE_PREFIX = "file:";

    private final String configPath;
    private final ResourceLoader resourceLoader;

    /**
     * 기본 생성자.
     * 설정 파일 경로와 ResourceLoader 없이 생성하며,
     * readConfig() 호출 시 기본 경로(DEFAULT_CONFIG_PATH)의 설정 파일을 읽는다.
     */
    public EgovCryptoConfigReader() {
        this(null, null);
    }

    /**
     * globals.properties 등에서 지정한 설정 파일 경로로 리더 생성
     * @param configPath 설정 파일 경로 (classpath:, file: 접두어 또는 일반 경로).
     *                   null 이거나 공백이면 기본 경로(DEFAULT_CONFIG_PATH)를 사용한다.
     * @param resourceLoader 설정 파일 로드 시 사용할 Spring ResourceLoader.
     *                       ResourceLoader가 존재하는 경우 최우선적으로 사용한다.
     */
    public EgovCryptoConfigReader(String configPath, ResourceLoader resourceLoader) {
        this.configPath = (configPath != null && !configPath.trim().isEmpty()) ? configPath.trim() : null;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 설정 파일을 읽어 EgovCryptoConfig 객체를 반환.
     * 지정 경로 → 기본 경로(DEFAULT_CONFIG_PATH) 순으로 찾고, 각 경로는
     * ResourceLoader → DefaultResourceLoader 순으로 시도한다. 모두 실패하면 기본 설정을 반환한다.
     */
    public EgovCryptoConfig readConfig() {
        InputStream inputStream = null;

        // 1. 지정 경로의 설정 파일 로드
        if (configPath != null) {
            inputStream = openStream(configPath);
            if (inputStream == null) {
                LOGGER.warn("Crypto configuration file not found: {}, fall back to default path", configPath);
            }
        }

        // 2. 지정 경로가 없거나 찾지 못한 경우 기본 경로의 설정 파일 로드
        if (inputStream == null) {
            inputStream = openStream(DEFAULT_CONFIG_PATH);
        }

        // 3. 설정 파일을 찾은 경우 UTF-8로 읽어 EgovCryptoConfig로 변환
        if (inputStream != null) {
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                Properties props = new Properties();
                props.load(reader);
                return mapPropertiesToBean(props, createDefaultConfig());
            } catch (IOException e) {
                LOGGER.warn("Failed to read crypto configuration file, use default config - {}", e.getMessage());
            }
        }

        // 4. 모두 실패한 경우 기본 설정 반환
        LOGGER.warn("Using default crypto configuration");
        return createDefaultConfig();
    }

    // 지정한 경로의 파일에 대한 InputStream을 연다
    private InputStream openStream(String path) {
        String location = toResourcePath(path);

        // 지정한 ResourceLoader가 있으면 해당 객체를 사용, 없으면 DefaultResourceLoader를 사용함
        InputStream is = openStreamWithResourceLoader(resourceLoader, location);
        return (is != null) ? is : openStreamWithResourceLoader(new DefaultResourceLoader(), location);
    }

    // ResourceLoader를 이용해 지정한 경로의 파일에 대한 InputStream을 연다
    @SuppressWarnings("null") // path는 null이 될 수 없으므로, getResource에 대한 null 타입 경고를 무시함
    private static InputStream openStreamWithResourceLoader(ResourceLoader loader, String path) {
        if (loader == null) return null;

        try {
            Resource resource = loader.getResource(path);
            return resource.isReadable() ? resource.getInputStream() : null;
        } catch (IOException e) {
            LOGGER.debug("Failed to open crypto configuration file: {} - {}", path, e.getMessage());
            return null;
        }
    }

    // 접두어가 없으면 파일이 실제로 존재할 때 file:, 아니면 classpath:로 해석한다
    private static String toResourcePath(String path) {
        // classpath:
        if (path.startsWith(CLASSPATH_PREFIX)) {
            // classpath: 문자열을 제거한 결과의 앞 뒤 공백을 모두 제거
            return CLASSPATH_PREFIX + path.substring(CLASSPATH_PREFIX.length()).trim();
        }

        // file:
        if (path.startsWith(FILE_PREFIX)) {
            // file: 문자열을 제거한 결과의 앞 뒤 공백을 모두 제거
            return FILE_PREFIX + path.substring(FILE_PREFIX.length()).trim();
        }

        File f = new File(path);
        if (f.isFile()) {
            return f.toURI().toString(); // "file:/..." (Windows 경로도 안전)
        }

        return CLASSPATH_PREFIX + path;
    }

    // 2026.02.28 KISA 보안취약점 조치
    // properties 설정 파일을 읽어 기존 bean(기본값이 채워진 상태) 위에 덮어쓴다.
    private static EgovCryptoConfig mapPropertiesToBean(Properties props, EgovCryptoConfig bean) {
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);

            // value가 null 이거나 key가 빈 문자열이면 예외가 날 수 있으므로 건너뜀
            if (value == null || key.isEmpty()) continue;
            String setterName = "set" + key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);

            // 2026.02.28 KISA 보안취약점 조치
            try {
                Method setter = findSetter(bean.getClass(), setterName);
                if (setter != null) {
                    Object arg = convertValue(value, setter.getParameterTypes()[0]);
                    setter.invoke(bean, arg);
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid number format for config property : " + key + " : " + e.getMessage(), e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid config property value for : " + key + " : " + e.getMessage(), e);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Failed to set config property : " + key + " : " + e.getMessage(), e);
            }
        }
        return bean;
    }

    // setter 메서드를 찾아서 반환
    private static Method findSetter(Class<?> beanClass, String setterName) {
        for (Method m : beanClass.getMethods()) {
            if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                return m;
            }
        }
        return null;
    }

    // 값(value)를 지정한 형식(targetType)으로 형변환 하여 반환
    private static Object convertValue(String value, Class<?> targetType) {
        if (targetType == boolean.class || targetType == Boolean.class) {
            /*
             * boolean 값은 문자열에 오타가 있으면 의도하지 않은 값이 설정될 수 있음
             * (EX. "ture" 문자열이 value로 들어가 있으면 이 값은 true가 아니라 false가 됨)
             * 따라서 의도하지 않은 동작을 줄이기 위해, 오타가 난 경우 예외를 발생시킴
             */
            String v = value.trim();
            if (!"true".equalsIgnoreCase(v) && !"false".equalsIgnoreCase(v)) {
                throw new IllegalArgumentException("Not a boolean: " + value);
            }
            
            return Boolean.parseBoolean(v);
        }

        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value.trim());
        }

        // Properties는 값의 앞쪽 공백만 제거하므로, 의도하지 않은 뒤쪽 공백이 남지 않도록 제거함
        return value.trim();
    }

    // 기본 설정 객체를 생성
    private static EgovCryptoConfig createDefaultConfig() {
        EgovCryptoConfig config = new EgovCryptoConfig();
        config.setId("egovCryptoConfig");
        config.setInitial(true);
        config.setCrypto(true);
        config.setAlgorithm("SHA-256");
        config.setAlgorithmKey("egovframe");
        config.setAlgorithmKeyHash("gdyYs/IZqY86VcWhT8emCYfqY1ahw2vtLG+/FzNqtrQ=");
        config.setCryptoBlockSize(1024);
        config.setCryptoPropertyLocation("classpath:/egovframework/egovProps/globals.properties");
        config.setPlainDigest(false);
        return config;
    }
}

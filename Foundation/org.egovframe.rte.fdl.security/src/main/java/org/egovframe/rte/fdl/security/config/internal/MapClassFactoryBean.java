/*
 * Copyright 2014 MOSPA(Ministry of Security and Public Administration).
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
package org.egovframe.rte.fdl.security.config.internal;

import org.egovframe.rte.fdl.logging.util.EgovJdkLogger;
import org.egovframe.rte.fdl.security.config.SecurityConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

/**
 * 사용자 정보 mapping 처리 클래스 지정을 처리하는 factory bean 클래스
 * 
 *<p>Desc.: 설정 간소화 처리에 사용되는 내부 factory bean</p>
 *
 * @author Vincent Han
 * @since 2014.03.12
 * @version 3.0
 * <pre>
 * 개정이력(Modification Information)
 *
 * 수정일		수정자				수정내용
 * ----------------------------------------------
 * 2014.03.12	한성곤				Spring Security 설정 간소화 기능 추가
 * </pre>
 */
public class MapClassFactoryBean implements FactoryBean<String>, ApplicationContextAware {

	private ApplicationContext context;
	
	private String defaultMapClass;

	public String getObject() throws Exception {
		try {
			SecurityConfig config = context.getBean(SecurityConfig.class);
			if (StringUtils.hasText(config.getJdbcMapClass())) {
				return config.getJdbcMapClass();
			}
		} catch (NoSuchBeanDefinitionException nsbde) {
			EgovJdkLogger.ignore("There is no SecurityConfig.class.");
		}
		
		return defaultMapClass;
	}

	public Class<String> getObjectType() {
		return String.class;
	}

	public boolean isSingleton() {
		return true;
	}
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	public String getDefaultMapClass() {
		return defaultMapClass;
	}

	@Required
	public void setDefaultMapClass(String defaultMapClass) {
		this.defaultMapClass = defaultMapClass;
	}

}

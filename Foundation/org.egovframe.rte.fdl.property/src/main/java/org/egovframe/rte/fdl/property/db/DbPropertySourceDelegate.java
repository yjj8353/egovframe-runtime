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
package org.egovframe.rte.fdl.property.db;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * DB기반의 PropertySource를 저장하는 클래스
 * @author yjLee
 */
public class DbPropertySourceDelegate {

	public static final String PROPERTY_SOURCE_KEY = "PKEY";
	public static final String PROPERTY_SOURCE_VALUE = "PVALUE";

	private Map<String, Object> properties = new HashMap<>();

	private JdbcTemplate jdbcTemplate;

	private String sql;

	public DbPropertySourceDelegate(DataSource dataSource, String sql) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.sql = sql;
		initProperties();
	}

	public void initProperties() {
		List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
		for (Map<String, Object> result : results) {
			if (result != null) {
				Iterator<String> iterator = result.keySet().iterator();
				String pKey = null;
				String pValue = null;
				while (iterator.hasNext()) {
					String key = iterator.next();
					String data = (String) result.get(key);
					if (PROPERTY_SOURCE_KEY.equals(key)) {
						pKey = data;
					} else if (PROPERTY_SOURCE_VALUE.equals(key)) {
						pValue = data;
					}
				}
				properties.put(pKey, pValue);
			}
		}
	}

	public Object getProperty(String key) {
		return this.properties.get(key);
	}

}

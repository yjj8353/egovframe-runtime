package org.egovframe.rte.fdl.property;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.*;

/**
 * Property Service 기본 기능 확인 클래스
 * @author 실행환경 개발팀 김태호
 * @since 2009.02.01
 * @version 1.0
 * @see <pre>
 *  == 개정이력(Modification Information) ==
 *   
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2009.02.01  김태호          최초 생성
 * 
 * </pre>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath*:/spring/context-common.xml",
        "classpath*:/spring/context-properties.xml"
})
public class PropertyServiceBasicTest {

    @Resource(name = "propertyService")
    protected EgovPropertyService propertyService;

    /**
     * 기본 처리 테스트
     * @throws Exception fail to test
     */
    @Test
    public void testPropertiesService() throws Exception {

        assertEquals("안녕하세요.", propertyService.getString("greet.message"));

        assertNotNull(propertyService.getString("tokens_on_multiple_lines"));

        assertEquals("1234", propertyService.getString("AAAA"));

        assertEquals(Double.valueOf(1234), Double.valueOf(propertyService.getDouble("number.double")));

        assertEquals(Float.valueOf(1234), Float.valueOf(propertyService.getFloat("number.float")));

        assertEquals(Integer.valueOf(1234), Integer.valueOf(propertyService.getInt("number.int")));

        assertEquals(Long.valueOf(1234), Long.valueOf(propertyService.getLong("number.long")));

        assertNotNull(propertyService.getString("AAAA", ""));

        assertEquals(Double.valueOf(1234), Double.valueOf(propertyService.getDouble("number.double", 123.4)));

        assertEquals(Float.valueOf(1234), Float.valueOf(propertyService.getFloat("number.float", (float) 123.4)));

        assertEquals(Integer.valueOf(1234), Integer.valueOf(propertyService.getInt("number.int", 123)));

        assertEquals(Long.valueOf(1234), Long.valueOf(propertyService.getLong("number.long", 1234)));

        assertNotNull(propertyService.getKeys());

        assertNotNull(propertyService.getKeys("number"));

        assertTrue(propertyService.getBoolean("boolean"));

        assertTrue(!propertyService.getBoolean("notexistboolean", false));

        assertEquals(2, propertyService.getStringArray("tokens_on_a_line").length);

        System.out.println(propertyService.getString("special.test"));

        assertEquals("~!@#$%^&*()_+;{}|", propertyService.getString("special.test"));

    }

}

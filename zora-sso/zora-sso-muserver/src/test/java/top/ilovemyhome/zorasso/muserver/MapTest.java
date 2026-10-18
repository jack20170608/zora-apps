package top.ilovemyhome.zorasso.muserver;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MapTest {

    @Test
    void test1(){
       Map<String, String> testMap = new HashMap<>();
       testMap.put("k1","v1");

        System.out.println(testMap.remove("k0", "v0"));
        System.out.println(testMap.remove("k1","v1"));
    }
}

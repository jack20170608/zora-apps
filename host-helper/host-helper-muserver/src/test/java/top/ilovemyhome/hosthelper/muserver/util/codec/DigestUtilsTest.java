package top.ilovemyhome.hosthelper.muserver.util.codec;

import org.junit.jupiter.api.Test;

public final class DigestUtilsTest {

    @Test
    public void testMd5Hex() {
        String input = "1";
        String hashHex = DigestUtils.sha256Hex(input);
        System.out.println(hashHex);
    }
}

package cn.hylexus.jt808.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * &copy; fanhuagang@gmail.com
 * Created by van on 2018/4/16.
 */
public class BCD8421OperaterTest {

    @Test
    public void bcd2String() {
        String number = "013912345678";
        //number = "13912345678";
        int length = number.length();
        byte[] bytes = new BCD8421Operater().string2Bcd(number);

        String s = new BCD8421Operater().bcd2String(bytes);

        assertEquals(number, s);

    }

    @Test
    public void string2Bcd() {
    }
}
package van.demo;

import cn.hylexus.jt808.service.codec.MsgDecoder;
import cn.hylexus.jt808.util.BCD8421Operater;
import cn.hylexus.jt808.util.HexStringUtils;
import cn.hylexus.jt808.vo.PackageData;
import cn.hylexus.jt808.vo.req.TerminalRegisterMsg;

/**
 * &copy; fanhuagang@gmail.com
 * Created by van on 2018/4/10.
 */
public class Test {
    public static void main(String[] args) {
        byte[] data = HexStringUtils.hexString2Bytes("0100002c0200000000150025002c0133373039363054372d54383038000000000000000000000000003033323931373001d4c142383838387b").getBytes();
        MsgDecoder msgDecoder = new MsgDecoder();
        PackageData bytes = msgDecoder.bytes2PackageData(data);
        TerminalRegisterMsg terminalRegisterMsg = msgDecoder.toTerminalRegisterMsg(bytes);
        System.out.println(terminalRegisterMsg);
    }
}

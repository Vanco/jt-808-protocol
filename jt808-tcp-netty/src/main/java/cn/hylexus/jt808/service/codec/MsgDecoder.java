package cn.hylexus.jt808.service.codec;

import cn.hylexus.jt808.vo.req.LocationInfoUploadMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hylexus.jt808.common.TPMSConsts;
import cn.hylexus.jt808.util.BCD8421Operater;
import cn.hylexus.jt808.util.BitOperator;
import cn.hylexus.jt808.vo.PackageData;
import cn.hylexus.jt808.vo.PackageData.MsgHeader;
import cn.hylexus.jt808.vo.req.TerminalRegisterMsg;
import cn.hylexus.jt808.vo.req.TerminalRegisterMsg.TerminalRegInfo;

import java.util.Arrays;

public class MsgDecoder {

	private static final Logger log = LoggerFactory.getLogger(MsgDecoder.class);

	private BitOperator bitOperator;
	private BCD8421Operater bcd8421Operater;

	public MsgDecoder() {
		this.bitOperator = new BitOperator();
		this.bcd8421Operater = new BCD8421Operater();
	}

	public PackageData bytes2PackageData(byte[] data) {
		PackageData ret = new PackageData();

		// 0. 终端套接字地址信息
		// ret.setChannel(msg.getChannel());

		// 1. 16byte 或 12byte 消息头
		MsgHeader msgHeader = this.parseMsgHeaderFromBytes(data);
		ret.setMsgHeader(msgHeader);

		int msgBodyByteStartIndex = 12;
		// 2. 消息体
		// 有子包信息,消息体起始字节后移四个字节:消息包总数(word(16))+包序号(word(16))
		if (msgHeader.isHasSubPackage()) {
			msgBodyByteStartIndex = 16;
		}

		byte[] tmp = new byte[msgHeader.getMsgBodyLength()];
		System.arraycopy(data, msgBodyByteStartIndex, tmp, 0, tmp.length);
		ret.setMsgBodyBytes(tmp);

		// 3. 去掉分隔符之后，最后一位就是校验码
		// int checkSumInPkg =
		// this.bitOperator.oneByteToInteger(data[data.length - 1]);
		int checkSumInPkg = data[data.length - 1];
		int calculatedCheckSum = this.bitOperator.getCheckSum4JT808(data, 0, data.length - 1);
		ret.setCheckSum(checkSumInPkg);
		if (checkSumInPkg != calculatedCheckSum) {
			log.warn("检验码不一致,msgid:0x{},pkg:{},calculated:{}", msgHeader.msgIdToHexString(), checkSumInPkg, calculatedCheckSum);
		}
		return ret;
	}

	private MsgHeader parseMsgHeaderFromBytes(byte[] data) {
		MsgHeader msgHeader = new MsgHeader();

		// 1. 消息ID word(16)
		// byte[] tmp = new byte[2];
		// System.arraycopy(data, 0, tmp, 0, 2);
		// msgHeader.setMsgId(this.bitOperator.twoBytesToInteger(tmp));
		msgHeader.setMsgId(this.parseIntFromBytes(data, 0, 2));

		// 2. 消息体属性 word(16)=================>
		// System.arraycopy(data, 2, tmp, 0, 2);
		// int msgBodyProps = this.bitOperator.twoBytesToInteger(tmp);
		int msgBodyProps = this.parseIntFromBytes(data, 2, 2);
		msgHeader.setMsgBodyPropsField(msgBodyProps);
		// [ 0-9 ] 0000,0011,1111,1111(3FF)(消息体长度)
		msgHeader.setMsgBodyLength(msgBodyProps & 0x3ff);
		// [10-12] 0001,1100,0000,0000(1C00)(加密类型)
		msgHeader.setEncryptionType((msgBodyProps & 0x1c00) >> 10);
		// [ 13_ ] 0010,0000,0000,0000(2000)(是否有子包)
		msgHeader.setHasSubPackage(((msgBodyProps & 0x2000) >> 13) == 1);
		// [14-15] 1100,0000,0000,0000(C000)(保留位)
		msgHeader.setReservedBit(((msgBodyProps & 0xc000) >> 14) + "");
		// 消息体属性 word(16)<=================

		// 3. 终端手机号 bcd[6]
		// tmp = new byte[6];
		// System.arraycopy(data, 4, tmp, 0, 6);
		// msgHeader.setTerminalPhone(this.bcd8421Operater.bcd2String(tmp));
		msgHeader.setTerminalPhone(this.parseBcdStringFromBytes(data, 4, 6));

		// 4. 消息流水号 word(16) 按发送顺序从 0 开始循环累加
		// tmp = new byte[2];
		// System.arraycopy(data, 10, tmp, 0, 2);
		// msgHeader.setFlowId(this.bitOperator.twoBytesToInteger(tmp));
		msgHeader.setFlowId(this.parseIntFromBytes(data, 10, 2));

		// 5. 消息包封装项
		// 有子包信息
		if (msgHeader.isHasSubPackage()) {
			// 消息包封装项字段
			msgHeader.setPackageInfoField(this.parseIntFromBytes(data, 12, 4));
			// byte[0-1] 消息包总数(word(16))
			// tmp = new byte[2];
			// System.arraycopy(data, 12, tmp, 0, 2);
			// msgHeader.setTotalSubPackage(this.bitOperator.twoBytesToInteger(tmp));
			msgHeader.setTotalSubPackage(this.parseIntFromBytes(data, 12, 2));

			// byte[2-3] 包序号(word(16)) 从 1 开始
			// tmp = new byte[2];
			// System.arraycopy(data, 14, tmp, 0, 2);
			// msgHeader.setSubPackageSeq(this.bitOperator.twoBytesToInteger(tmp));
			msgHeader.setSubPackageSeq(this.parseIntFromBytes(data, 12, 2));
		}
		return msgHeader;
	}

	protected String parseStringFromBytes(byte[] data, int startIndex, int lenth) {
		return this.parseStringFromBytes(data, startIndex, lenth, null);
	}

	private String parseStringFromBytes(byte[] data, int startIndex, int lenth, String defaultVal) {
		try {
			byte[] tmp = new byte[lenth];
			System.arraycopy(data, startIndex, tmp, 0, lenth);
			return new String(tmp, TPMSConsts.string_charset);
		} catch (Exception e) {
			log.error("解析字符串出错:{}", e.getMessage());
			e.printStackTrace();
			return defaultVal;
		}
	}

	private String parseBcdStringFromBytes(byte[] data, int startIndex, int lenth) {
		return this.parseBcdStringFromBytes(data, startIndex, lenth, null);
	}

	private String parseBcdStringFromBytes(byte[] data, int startIndex, int lenth, String defaultVal) {
		try {
			byte[] tmp = new byte[lenth];
			System.arraycopy(data, startIndex, tmp, 0, lenth);
			return this.bcd8421Operater.bcd2String(tmp);
		} catch (Exception e) {
			log.error("解析BCD(8421码)出错:{}", e.getMessage());
			e.printStackTrace();
			return defaultVal;
		}
	}

	private int parseIntFromBytes(byte[] data, int startIndex, int length) {
		return this.parseIntFromBytes(data, startIndex, length, 0);
	}

	private int parseIntFromBytes(byte[] data, int startIndex, int length, int defaultVal) {
		try {
			// 字节数大于4,从起始索引开始向后处理4个字节,其余超出部分丢弃
			final int len = length > 4 ? 4 : length;
			byte[] tmp = new byte[len];
			System.arraycopy(data, startIndex, tmp, 0, len);
			return bitOperator.byteToInteger(tmp);
		} catch (Exception e) {
			log.error("解析整数出错:{}", e.getMessage());
			e.printStackTrace();
			return defaultVal;
		}
	}

	public TerminalRegisterMsg toTerminalRegisterMsg(PackageData packageData) {
		TerminalRegisterMsg ret = new TerminalRegisterMsg(packageData);
		byte[] data = ret.getMsgBodyBytes();

		TerminalRegInfo body = new TerminalRegInfo();

		// 1. byte[0-1] 省域ID(WORD)
		// 设备安装车辆所在的省域，省域ID采用GB/T2260中规定的行政区划代码6位中前两位
		// 0保留，由平台取默认值
		body.setProvinceId(this.parseIntFromBytes(data, 0, 2));

		// 2. byte[2-3] 设备安装车辆所在的市域或县域,市县域ID采用GB/T2260中规定的行 政区划代码6位中后四位
		// 0保留，由平台取默认值
		body.setCityId(this.parseIntFromBytes(data, 2, 2));

		// 3. byte[4-8] 制造商ID(BYTE[5]) 5 个字节，终端制造商编码
		// byte[] tmp = new byte[5];
		body.setManufacturerId(this.parseStringFromBytes(data, 4, 5));

		// 4. byte[9-16] 终端型号(BYTE[8]) 八个字节， 此终端型号 由制造商自行定义 位数不足八位的，补空格。
		body.setTerminalType(this.parseStringFromBytes(data, 9, 8));

		// 5. byte[17-23] 终端ID(BYTE[7]) 七个字节， 由大写字母 和数字组成， 此终端 ID由制造 商自行定义
		body.setTerminalId(this.parseStringFromBytes(data, 17, 7));

		// 6. byte[24] 车牌颜色(BYTE) 车牌颜 色按照JT/T415-2006 中5.4.12 的规定
		body.setLicensePlateColor(this.parseIntFromBytes(data, 24, 1));

		// 7. byte[25-x] 车牌(STRING) 公安交 通管理部门颁 发的机动车号牌
		body.setLicensePlate(this.parseStringFromBytes(data, 25, data.length - 25));

		ret.setTerminalRegInfo(body);
		return ret;
	}


	public LocationInfoUploadMsg toLocationInfoUploadMsg(PackageData packageData) {
		LocationInfoUploadMsg ret = new LocationInfoUploadMsg(packageData);
		final byte[] data = ret.getMsgBodyBytes();

		// 1. byte[0-3] 报警标志(DWORD(32))
		ret.setWarningFlagField(this.parseIntFromBytes(data, 0, 3));
		// 2. byte[4-7] 状态(DWORD(32))
		ret.setStatusField(this.parseIntFromBytes(data, 4, 4));
		// 3. byte[8-11] 纬度(DWORD(32)) 以度为单位的纬度值乘以10^6，精确到百万分之一度
		ret.setLatitude(this.parseFloatFromBytes(data, 8, 4));
		// 4. byte[12-15] 经度(DWORD(32)) 以度为单位的经度值乘以10^6，精确到百万分之一度
		ret.setLongitude(this.parseFloatFromBytes(data, 12, 4));
		// 5. byte[16-17] 高程(WORD(16)) 海拔高度，单位为米（ m）
		ret.setElevation(this.parseIntFromBytes(data, 16, 2));
		// byte[18-19] 速度(WORD) 1/10km/h
		ret.setSpeed(this.parseFloatFromBytes(data, 18, 2));
		// byte[20-21] 方向(WORD) 0-359，正北为 0，顺时针
		ret.setDirection(this.parseIntFromBytes(data, 20, 2));
		// byte[22-x] 时间(BCD[6]) YY-MM-DD-hh-mm-ss
		// GMT+8 时间，本标准中之后涉及的时间均采用此时区
		// ret.setTime(this.par);

		byte[] tmp = new byte[6];
		System.arraycopy(data, 22, tmp, 0, 6);
		String time = this.parseBcdStringFromBytes(data, 22, 6);
		return ret;
	}

	public String toBodyString(PackageData packageData) {
		StringBuilder sb = new StringBuilder();

		MsgHeader msgHeader = packageData.getMsgHeader();
		int msgId = msgHeader.getMsgId();

		byte[] data = packageData.getMsgBodyBytes();

		switch (msgId) {
			case 0x8f41:
				sb.append("Version:").append(this.parseIntFromBytes(data, 0, 1)).append("\n");
				sb.append("Time:").append(this.parseBcdStringFromBytes(data, 1, 6)).append("\n");
				int cmdLen = this.parseIntFromBytes(data, 7, 1);
				sb.append("Commands:").append(cmdLen).append("\n");
				for (int i = 0, offset = 0; i < cmdLen; i++) {
					sb.append("\tCmdNo").append(i).append(":").append(this.parseIntFromBytes(data, 8 + offset, 1)).append("\n");
					sb.append("\tCmdInstruction").append(i).append(":").append(this.parseIntFromBytes(data, 9 + offset, 1)).append("\n");
					sb.append("\tCmdParam").append(i).append(":").append(this.parseIntFromBytes(data, 10 + offset, 1)).append("\n");
					offset += 3;
				}
				break;
			case 0x0f41:
				sb.append("Version:").append(this.parseIntFromBytes(data, 0, 1)).append("\n");
				sb.append("Time:").append(this.parseBcdStringFromBytes(data, 1, 6)).append("\n");
				sb.append("FlowId:").append(this.parseIntFromBytes(data, 7, 2)).append("\n");
				cmdLen = this.parseIntFromBytes(data, 9, 1);
                sb.append("Commands:").append(cmdLen).append("\n");
                for (int i = 0, offset = 0; i < cmdLen; i++) {
					sb.append("\tCmdNo").append(i).append(":").append(this.parseIntFromBytes(data, 10 + offset, 1)).append("\n");
					sb.append("\tCmdInstruction").append(i).append(":").append(this.parseIntFromBytes(data, 11 + offset, 1)).append("\n");
					sb.append("\tCmdStatus").append(i).append(":").append(this.parseIntFromBytes(data, 12 + offset, 1)).append("\n");
					offset += 3;
				}
				break;
			case 0x8f51:
				sb.append("Version:").append(this.parseIntFromBytes(data, 0, 1)).append("\n");
				sb.append("Time:").append(this.parseBcdStringFromBytes(data, 1, 6)).append("\n");
				cmdLen = this.parseIntFromBytes(data, 7, 1);
				for (int i = 0, offset = 0; i < cmdLen; i++) {
					sb.append("\tCmdNo").append(i).append(":").append(this.parseIntFromBytes(data, 8 + offset, 1)).append("\n");
					offset ++;
				}
				break;
			case 0x0f51:
				sb.append("Version:").append(this.parseIntFromBytes(data, 0, 1)).append("\n");
				sb.append("Time:").append(this.parseBcdStringFromBytes(data, 1, 6)).append("\n");
				sb.append("PackageFlag:").append(this.parseIntFromBytes(data, 7, 1)).append("\n");
				sb.append("FlowId:").append(this.parseIntFromBytes(data, 8, 2)).append("\n");
				cmdLen = this.parseIntFromBytes(data, 10, 1);
				for (int i = 0, offset = 0; i < cmdLen; i++) {
					int cmdNo = this.parseIntFromBytes(data, 11 + offset, 1);
					sb.append("\tCmdNo").append(i).append(":").append(cmdNo).append("\n");
					int resultLen = this.parseIntFromBytes(data, 12 + offset, 1);
					sb.append("\tCmdResult").append(i).append(":").append("\n");
					byte[] resultBytes = Arrays.copyOfRange(data, 13 + offset, resultLen);
					sb.append(expend0f51Result(cmdNo, resultLen, resultBytes));
					offset += 2 + resultLen;
				}
				break;
            case 0x9102:
                sb.append("Time:").append(this.parseBcdStringFromBytes(data, 0, 6)).append("\n");
                sb.append("Mileage:").append(this.parseIntFromBytes(data, 6, 4)).append("\n");
                sb.append("AccumulateMileage:").append(this.parseIntFromBytes(data, 10, 4)).append("\n");
                sb.append("AccumulateFuel:").append(this.parseIntFromBytes(data, 14, 4));
                break;
            case 0x0f3a:
                sb.append("Version:").append("\n");
                int v = this.parseIntFromBytes(data, 0, 1);
                sb.append("\tF3A Version:").append(bitOperator.getBitRange(v, 0, 6)).append("\n");
                sb.append("\tRetransmission?:").append(bitOperator.getBitAt(v, 7));

                sb.append("Latitude:").append(this.parseIntFromBytes(data, 1, 4)).append("\n");
                sb.append("Longitude:").append(this.parseIntFromBytes(data, 5, 4)).append("\n");
                sb.append("Attitude:").append(this.parseIntFromBytes(data, 9, 2)).append("\n");
                sb.append("Direction:").append(this.parseIntFromBytes(data, 11, 2)).append("\n");
                sb.append("GpsTime:").append(this.parseBcdStringFromBytes(data, 13, 6)).append("\n");

                int secondPkgNum = this.parseIntFromBytes(data, 19, 1);
                if (secondPkgNum > 10) {
                    log.warn("二级包个数超过（10）：{}", secondPkgNum);
                }

                sb.append("Level 2 package number:").append(secondPkgNum).append("\n");
                for (int i = 0, offset = 0; i < secondPkgNum; i ++) {
                    sb.append("\tFaultInfoType").append(i).append(":").append(this.parseIntFromBytes(data, 20 + offset, 1)).append("\n");
					int infoLen = this.parseIntFromBytes(data, 21 + offset, 1);
					sb.append("\tFaultInfoLength").append(i).append(":").append(infoLen).append("\n");
                    sb.append("\tSpeed").append(i).append(":").append(this.parseIntFromBytes(data, 22 + offset, 2)).append("*1/256\n");
                    sb.append("\tGasolineThrottle").append(i).append(":").append(this.parseIntFromBytes(data, 24 + offset, 1)).append("*0.4%\n");
                    sb.append("\tBrakeSignal").append(i).append(":").append(this.parseIntFromBytes(data, 25 + offset, 1)).append("\n");
                    sb.append("\tEngineSpeed").append(i).append(":").append(this.parseIntFromBytes(data, 26 + offset, 2)).append("*0.125RPM\n");
                    sb.append("\tTurbocharged engine pressure").append(i).append(":").append(this.parseIntFromBytes(data, 28 + offset, 1)).append("*2 KPa\n");
					sb.append("\tEngine intake pressure").append(i).append(":").append(this.parseIntFromBytes(data, 29 + offset, 1)).append("*2 KPa\n");
					sb.append("\tEngine exhaust temperature").append(i).append(":").append(this.parseIntFromBytes(data,30 + offset, 2)).append("*0.03125 ℃\n");
					sb.append("\tEngine water temperature").append(i).append(":").append(this.parseIntFromBytes(data, 32 +  offset, 1)).append("-40 ℃\n");
					sb.append("\tGasolineThrottleChangeRate").append(i).append(":").append(this.parseIntFromBytes(data, 33 + offset, 2)).append("%/s\n");
					sb.append("\tGear").append(i).append(":").append(this.parseIntFromBytes(data, 35 + offset, 1)).append("-125\n");
					sb.append("\tEngine output torque").append(i).append(":").append(this.parseIntFromBytes(data, 36 + offset, 1)).append("-125%\n");
					sb.append("\tLoading").append(i).append(":").append(this.parseIntFromBytes(data, 37 + offset, 4)).append("kg\n");
					sb.append("\tEngineLoad").append(i).append(":").append(this.parseIntFromBytes(data, 41 + offset, 1)).append("%\n");
					sb.append("\tAcceleration").append(i).append(":").append(this.parseIntFromBytes(data, 42 + offset, 2)).append("*0.01 m/s\n");
					sb.append("\tDeceleration").append(i).append(":").append(this.parseIntFromBytes(data, 44 + offset, 2)).append("*0.01 m/s\n");

					int faultNum = this.parseIntFromBytes(data, 46 + offset, 1);
					sb.append("\tFaultNumber").append(i).append(":").append(faultNum).append("\n");
					for (int j = 0, p = 0; j < faultNum; j ++) {
						sb.append("\t\tSoureAddr").append(j).append(":").append(this.parseIntFromBytes(data, 47 + offset + p, 1)).append("\n");
						sb.append("\t\tSPN").append(j).append(":").append(this.parseIntFromBytes(data, 48 + offset + p, 4)).append("\n");
						sb.append("\t\tFMI").append(j).append(":").append(this.parseIntFromBytes(data, 50 + offset + p, 1)).append("\n");
						p += 6;
					}

					offset += 1 + infoLen;
                }
                break;

			case 0x0f3b:
				sb.append("Version:").append("\n");
				v = this.parseIntFromBytes(data, 0, 1);
				sb.append("\tF3A Version:").append(bitOperator.getBitRange(v, 0, 6)).append("\n");
				sb.append("\tRetransmission?:").append(bitOperator.getBitAt(v, 7)).append("\n");
				int pkgNum = this.parseIntFromBytes(data, 1, 1);
				sb.append("\tPackageNum:").append(pkgNum).append("\n");

				for (int i = 0, offset = 0; i < pkgNum; i ++) {
					//GPS
					sb.append("\t\tLatitude:").append(this.parseIntFromBytes(data, 2 + offset, 4)).append("\n");
					sb.append("\t\tLongitude:").append(this.parseIntFromBytes(data, 6 + offset, 4)).append("\n");
					sb.append("\t\tAltitude:").append(this.parseIntFromBytes(data, 10 + offset, 2)).append("\n");
					sb.append("\t\tDirection:").append(this.parseIntFromBytes(data, 12 + offset, 2)).append("\n");
					sb.append("\t\tGpsTime:").append(this.parseBcdStringFromBytes(data, 14 + offset, 6)).append("\n");

					int pkgNum2 =  bitOperator.getBitRange(this.parseIntFromBytes(data, 20 + offset, 1),0, 3);

					sb.append("\t\tSecondPkgNumber:").append(pkgNum2).append("\n");
					int p = 0;
					for (int j = 0; j < pkgNum2; j ++) {
						sb.append("\t\t").append(j).append("==>").append("\n");
						sb.append("\t\t\tEngineSpeed:").append(this.parseIntFromBytes(data, 21 + offset + p, 2)).append("\n");
						sb.append("\t\t\tCarMeterSpeed:").append(this.parseIntFromBytes(data, 23 + offset + p, 2)).append("\n");
						sb.append("\t\t\tWheelSpeed:").append(this.parseIntFromBytes(data, 25 + offset + p, 2)).append("\n");
						sb.append("\t\t\tGpsSpeed:").append(this.parseIntFromBytes(data, 27 + offset + p, 2)).append("\n");
						sb.append("\t\t\tGasolineThrottle:").append(this.parseIntFromBytes(data, 29 + offset + p, 1)).append("\n");
						sb.append("\t\t\tEngine output torque:").append(this.parseIntFromBytes(data, 30 + offset + p, 1)).append("\n");

						int tmp = this.parseIntFromBytes(data, 31 + offset + p, 1);
						sb.append("\t\t\tParking Brake Switch:").append(bitOperator.getBitRange(tmp, 0, 1)).append("\n");
						sb.append("\t\t\tBrake Switch:").append(bitOperator.getBitRange(tmp, 2, 3)).append("\n");
						sb.append("\t\t\tClutch switch:").append(bitOperator.getBitRange(tmp, 4, 5)).append("\n");
						sb.append("\t\t\tCruise control setting switch:").append(bitOperator.getBitRange(tmp, 6, 7)).append("\n");

						sb.append("\t\t\tCurrent Gear:").append(this.parseIntFromBytes(data, 32 + offset + p, 1)).append("\n");
						sb.append("\t\t\tTarget Gear:").append(this.parseIntFromBytes(data, 33 + offset + p, 1)).append("\n");

						sb.append("\t\t\tEngine fuel consumption rate:").append(this.parseIntFromBytes(data, 34 + offset + p, 2)).append("\n");
						sb.append("\t\t\tSlope:").append(this.parseIntFromBytes(data, 36 + offset + p, 2)).append("\n");
						sb.append("\t\t\tLoading:").append(this.parseIntFromBytes(data, 38 + offset + p, 2)).append("\n");
						sb.append("\t\t\tFuel Level:").append(this.parseIntFromBytes(data, 40 + offset + p, 1)).append("\n");
						sb.append("\t\t\tWater Temperature:").append(this.parseIntFromBytes(data, 41 + offset + p, 1)).append("\n");
						sb.append("\t\t\tAtmospheric pressure:").append(this.parseIntFromBytes(data, 42 + offset + p, 1)).append("\n");
						sb.append("\t\t\tIntake Pressure:").append(this.parseIntFromBytes(data, 43 + offset + p, 2)).append("\n");
						sb.append("\t\t\tAtmospheric Temperature:").append(this.parseIntFromBytes(data, 45 + offset + p, 1)).append("\n");
						sb.append("\t\t\tExhaust Temperature:").append(this.parseIntFromBytes(data, 46 + offset + p, 2)).append("\n");
						sb.append("\t\t\tIntake manifold boost pressure:").append(this.parseIntFromBytes(data, 48 + offset + p, 2)).append("\n");
						sb.append("\t\t\tRelative pressure:").append(this.parseIntFromBytes(data, 50 + offset + p, 2)).append("\n");
						sb.append("\t\t\tEngine torque mode:").append(this.parseIntFromBytes(data, 52 + offset + p, 1)).append("\n");
						sb.append("\t\t\thydraulics pressure:").append(this.parseIntFromBytes(data, 53 + offset + p, 2)).append("\n");

						sb.append("\t\t\tUrea level:").append(this.parseIntFromBytes(data, 55 + offset + p, 1)).append("\n");
						sb.append("\t\t\tStatus Flag:").append(this.parseIntFromBytes(data, 56 + offset + p, 4)).append("\n"); //todo

						int extLen = this.parseIntFromBytes(data, 60 + offset + p, 1);
						sb.append("\t\t\tExtLength:").append(extLen).append("\n");
						if (extLen > 0) {
							sb.append("\t\t\tExtData:").append(Arrays.toString(Arrays.copyOfRange(data, 61 + offset + p, 61 + offset + p + extLen -1))).append("\n");
						}

						p += 40 + extLen;

					}

					offset += 19 + p;
				}



				break;

			case 0x2035:
				sb.append("VIN:").append(this.parseStringFromBytes(data, 0, 17)).append("\n");
				sb.append("ECU-ID:").append(this.parseBcdStringFromBytes(data, 17, 12)).append("\n");
				sb.append("VAN:").append(this.parseStringFromBytes(data, 29, 10)).append("\n");
				sb.append("SN:").append(this.parseStringFromBytes(data, 39, 32)).append("\n");
				sb.append("Plate Number:").append(this.parseStringFromBytes(data, 71, msgHeader.getMsgBodyLength() - 71 -1));
				break;
				default: sb.append(Arrays.toString(data)).append("\n");
		}

		return sb.toString();
	}

	private String expend0f51Result(int cmdNo, int resultLen, byte[] resultBytes) {
		StringBuilder sb = new StringBuilder();
		switch (cmdNo) {
			case 0x02: //Door
				assert resultLen == 2;
				int result = bitOperator.twoBytesToInteger(resultBytes);
				sb.append("\t\tMainDriveDoor:").append(bitOperator.getBitAt(result, 0)).append("\n");
				sb.append("\t\tMainDriveDoorValid:").append(bitOperator.getBitAt(result, 1)).append("\n");
				sb.append("\t\tDeputyDriveDoor:").append(bitOperator.getBitAt(result, 2)).append("\n");
				sb.append("\t\tDeputyDriveDoorValid:").append(bitOperator.getBitAt(result, 3)).append("\n");
				sb.append("\t\tMainRearDoor:").append(bitOperator.getBitAt(result, 4)).append("\n");
				sb.append("\t\tMainRearDoorValid:").append(bitOperator.getBitAt(result, 5)).append("\n");
				sb.append("\t\tDeputyRearDoor:").append(bitOperator.getBitAt(result, 6)).append("\n");
				sb.append("\t\tDeputyRearDoorValid:").append(bitOperator.getBitAt(result, 7)).append("\n");
				sb.append("\t\tTailGate:").append(bitOperator.getBitAt(result, 8)).append("\n");
				sb.append("\t\tTailGateValid:").append(bitOperator.getBitAt(result, 9)).append("\n");
				sb.append("\t\tPowerDoorLocks:").append(bitOperator.getBitAt(result, 14)).append("\n");
				sb.append("\t\tPowerDoorLocksValid:").append(bitOperator.getBitAt(result, 15)).append("\n");

				break;
			case 0x05: //Windows
				assert resultLen == 4;
				sb.append("\t\tMainWindow:").append(resultBytes[0]).append("\n");
				sb.append("\t\tDeputyWindow:").append(resultBytes[1]).append("\n");
				sb.append("\t\tMainRearWindow:").append(resultBytes[2]).append("\n");
				sb.append("\t\tDeputyRearWindow:").append(resultBytes[3]).append("\n");
				break;
			case 0x07: // Sunroof
				assert resultLen == 2;
				sb.append("\t\tSunroof 1:").append(resultBytes[0]).append("\n");
				sb.append("\t\tSunroof 2:").append(resultBytes[1]).append("\n");
				break;

			case 0x09: // Light
				int lightNum = resultBytes[0];
				for (int i = 0, p = 1; i < lightNum; i ++) {
				    sb.append("\t\t").append(lightIdToName(resultBytes[p++])).append(resultBytes[p++]).append("\n");
                }
				break;
			case 0x0b: //Financial Lock
                assert resultLen == 1;
                sb.append("\t\tFinancialLock:").append(resultBytes[0]).append("\n");
				break;
			case 0x0d: //Financial lock active
                assert resultLen == 1;
                sb.append("\t\tFinancialLockActive:").append(resultBytes[0]).append("\n");
				break;
			case 0x0f: //Engine
                assert resultLen == 1;
                sb.append("\t\tEngine:").append(resultBytes[0]).append("\n");
				break;
			case 0x11: //Acc
                assert resultLen == 1;
                sb.append("\t\tACC:").append(resultBytes[0]).append("\n");
				break;
			case 0x13: // Wifi
                assert resultLen == 1;
                sb.append("\t\tWIFI:").append(resultBytes[0]).append("\n");
				break;
			case 0x14: // A/C
                assert resultLen == 5;
                sb.append("\t\tA/C ON/OFF Status:").append(resultBytes[0]).append("\n");
                sb.append("\t\tTemperature:").append(resultBytes[1]).append("\n");
                sb.append("\t\tFrontDefrost:").append(resultBytes[2]).append("\n");
                sb.append("\t\tRearDefrost:").append(resultBytes[3]).append("\n");
                sb.append("\t\tRearViewMirror:").append(resultBytes[4]).append("\n");
				break;
			case 0x15: // Windscreen wiper
                assert resultLen == 2;
                sb.append("\t\tFrontWindScreenWiper:").append(resultBytes[0]).append("\n");
                sb.append("\t\tRearWindScreenWiper:").append(resultBytes[1]).append("\n");
				break;
			case 0x16: // Rear view mirror
                assert resultLen == 2;
                sb.append("\t\tDriveSideWingMirror:").append(resultBytes[0]).append("\n");
                sb.append("\t\tPassengerSideWingMirror:").append(resultBytes[1]).append("\n");
				break;
		}

		return sb.toString();
	}

    private String lightIdToName(byte lid) {
	    switch (lid) {
            case 1: return "DippedBeam:";
            case 2: return "MainBeam:";
            case 3: return "TurnLeft:";
            case 4: return "TurnRight:";
            case 5: return "HazardFlasher:";
            case 6: return "IdentificationLamp:";
            case 7: return "BrakeLamp:";
            case 8: return "FrontFogLamp:";
            case 9: return "RearFogLamp:";
            case 10: return "VariousSmallLamp:";
            case 11: return "DaytimeRunningLamp:";
            default: return "";
        }
    }

    private float parseFloatFromBytes(byte[] data, int startIndex, int length) {
		return this.parseFloatFromBytes(data, startIndex, length, 0f);
	}

	private float parseFloatFromBytes(byte[] data, int startIndex, int length, float defaultVal) {
		try {
			// 字节数大于4,从起始索引开始向后处理4个字节,其余超出部分丢弃
			final int len = length > 4 ? 4 : length;
			byte[] tmp = new byte[len];
			System.arraycopy(data, startIndex, tmp, 0, len);
			return bitOperator.byte2Float(tmp);
		} catch (Exception e) {
			log.error("解析浮点数出错:{}", e.getMessage());
			e.printStackTrace();
			return defaultVal;
		}
	}
}

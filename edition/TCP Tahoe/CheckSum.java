package com.ouc.tcp.test;

import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

/**
 * 使用 CRC 循环冗余校验码
 * CRC32 的 update 方法可以用指定字节来更新校验和
 * CRC32 的 getValue 方法可以获取校验和
 */

public class CheckSum {

	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		CRC32 crc32 = new CRC32();
		TCP_HEADER header = tcpPack.getTcpH();  // 获取原TCP报文头部
		crc32.update(header.getTh_seq());  // 添加seq进行校验
		crc32.update(header.getTh_ack());  // 添加ack进行校验

		// 添加 TCP 数据字段进行校验
		for (int i = 0; i < tcpPack.getTcpS().getData().length; i ++ )
		{
			crc32.update(tcpPack.getTcpS().getData()[i]);
		}

		// 获取校验码，并返回
		return (short) crc32.getValue();
	}
	
}

package com.ouc.tcp.test;

import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {
	
	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		int checkSum = 0;
		TCP_HEADER header = tcpPack.getTcpH();

		//1.seq字段
		int seq = header.getTh_seq();
		checkSum += seq & 0xFFFF;
		checkSum += (seq >> 16) & 0xFFFF;
		//2.ack字段
		int ack = header.getTh_ack();
		checkSum += ack & 0xFFFF;
		checkSum += (ack >> 16) & 0xFFFF;
		//3.数据字段
		int[] segment = tcpPack.getTcpS().getData();
		if(segment != null)
		{
			for(int i=0;i<segment.length;i++)
			{
				int a = segment[i];
				checkSum += a & 0xFFFF;
				checkSum += (a >> 16) & 0xFFFF;
			}
		}
		while ((checkSum >> 16)!=0)
		{
			checkSum = (checkSum >> 16) + (checkSum & 0xFFFF);
		}
		checkSum = ~checkSum;

		return (short) (checkSum & 0xFFFF);
	}
	
}

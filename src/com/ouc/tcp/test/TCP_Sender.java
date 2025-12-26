/***************************2.1: ACK/NACK
**************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class TCP_Sender extends TCP_Sender_ADT {

	private TCP_PACKET tcpPack;	//待发送的TCP数据报
	private volatile int flag = 0;

	private volatile int base = 1; //最早未确认包序号
	private volatile int nextSeqNum = 1; //下一个待发送包的序号
	private int windowSize = 4;
	private ConcurrentHashMap<Integer, TCP_PACKET> sentPacks = new ConcurrentHashMap<>();

	private  UDT_Timer timer = null;
	/*构造函数*/
	public TCP_Sender() {
		super();	//调用超类构造函数
		super.initTCP_Sender(this);		//初始化TCP发送端
	}

	@Override
	//可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改

	public void rdt_send(int dataIndex, int[] appData) {

		int currentSeq = dataIndex * appData.length + 1;
		// 创建新的 TCP_HEADER 和 TCP_SEGMENT（避免对象复用）
		TCP_HEADER newTcpH = new TCP_HEADER();
		newTcpH.setTh_sport(tcpH.getTh_sport());
		newTcpH.setTh_dport(tcpH.getTh_dport());
		newTcpH.setTh_seq(currentSeq);

		TCP_SEGMENT newTcpS = new TCP_SEGMENT();
		newTcpS.setData(appData);

		// 生成TCP数据报
		tcpPack = new TCP_PACKET(newTcpH, newTcpS, destinAddr);
		newTcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(newTcpH);

		/*//生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
		tcpH.setTh_seq(currentseq);//包序号设置为字节流号：

		tcpS.setData(appData);
		tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
		//更新带有checksum的TCP 报文头
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);*/

		while(nextSeqNum >= base + windowSize * 100) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				}
		}
		//保存TCP数据报
		sentPacks.put(currentSeq, tcpPack);
		//发送TCP数据报
		udt_send(tcpPack);

		if(nextSeqNum == base){
			startTimer();
		}
		nextSeqNum += 100;

	}

	private synchronized void startTimer(){
		if(timer == null) {
			timer = new UDT_Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					//重传
					for(int i = base; i < nextSeqNum; i++) {
						System.out.println("[GBN] Retransmit packet seq: " + i);
						udt_send(sentPacks.get(i));
					}
				}
			}, 1000);
		}
	}

	private synchronized void stopTimer(){
		if(timer != null){
			timer.cancel();
			timer = null;
		}

	}

	@Override
	//不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
	public void udt_send(TCP_PACKET stcpPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)7);
		//System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());
		//发送数据报
		client.send(stcpPack);
	}

	@Override
	//需要修改
	public void waitACK() {

		//循环检查ackQueue
		//循环检查确认号对列中是否有新收到的ACK		
		while (!ackQueue.isEmpty()){
			int currentAck=ackQueue.poll();
			// System.out.println("CurrentAck: "+currentAck);
			if (currentAck >= base){
				base = currentAck + 100;
				stopTimer();
				if(base < nextSeqNum){
					startTimer();
				}
				//break;
			}else{
				System.out.println("Retransmit: "+tcpPack.getTcpH().getTh_seq());
			}
		}
	}
	@Override
	//接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为－1；不需要修改
	public void recv(TCP_PACKET recvPack) {
		if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum())
		{
			//校验和正确
			System.out.println("Receive ACK Number： "+ recvPack.getTcpH().getTh_ack());
			ackQueue.add(recvPack.getTcpH().getTh_ack());
			//处理ACK报文
			waitACK();
		}
		else
		{
			//校验和错误，ACK损坏。
			System.out.println("Receive Corrupted ACK, ignored");
		}
	    System.out.println();

	}

}

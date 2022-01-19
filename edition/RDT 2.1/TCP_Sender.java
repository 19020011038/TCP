/***************************2.1: ACK/NACK
**************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Sender extends TCP_Sender_ADT {
	
	private TCP_PACKET tcpPack;	//待发送的TCP数据报
	private volatile int flag = 0;
	
	/*构造函数*/
	public TCP_Sender() {
		super();	//调用超类构造函数
		super.initTCP_Sender(this);		//初始化TCP发送端
	}
	
	@Override
	//可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改
	public void rdt_send(int dataIndex, int[] appData) {
		
		//生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
		tcpH.setTh_seq(dataIndex * appData.length + 1);//包序号设置为字节流号：
		tcpS.setData(appData);
		tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);		
		//更新带有checksum的TCP 报文头		
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		
		//发送TCP数据报
		udt_send(tcpPack);
		flag = 0;
		
		//等待ACK报文
		//waitACK();
		while (flag==0);
	}
	
	@Override
	//不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
	public void udt_send(TCP_PACKET stcpPack) {
		//设置错误控制标志
		// 设置错误控制标志
		// 0: 信道无差错
		// 1: 只出错
		// 2: 只丢包
		// 3: 只延迟
		// 4: 出错 / 丢包
		// 5: 出错 / 延迟
		// 6: 丢包 / 延迟
		// 7: 出错 / 丢包 / 延迟
		tcpH.setTh_eflag((byte)1);
		//System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());				
		//发送数据报
		client.send(stcpPack);
	}
	
	@Override
	//需要修改
	public void waitACK() {
		//循环检查ackQueue
		//循环检查确认号队列中是否有新收到的ACK
		if(!ackQueue.isEmpty()){
			int currentAck=ackQueue.poll();
			// System.out.println("CurrentAck: "+currentAck);
			if (currentAck == -1){  // NACK
				System.out.println("Retransmit: "+tcpPack.getTcpH().getTh_seq());
				udt_send(tcpPack);  // 重新发包
				flag = 0; // 仍然是waitACK状态
			}else{  // ACK
				System.out.println("Clear: "+tcpPack.getTcpH().getTh_seq());
				flag = 1; // 切换为等待应用层调用状态
				//break;
			}
		}
	}

	@Override
	//接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为－1；不需要修改
	public void recv(TCP_PACKET recvPack) {
		if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {  // 检查校验和
			System.out.println("Receive ACK Number： "+ recvPack.getTcpH().getTh_ack());
			ackQueue.add(recvPack.getTcpH().getTh_ack());  // 插入ack队列
			System.out.println();
		} else {
			System.out.println("Receive corrupt ACK: " + recvPack.getTcpH().getTh_ack());
			this.ackQueue.add(-1);  // ACK或NACK出错，在ack队列插入-1
			System.out.println();
		}

		// 处理 ACK 报文
		waitACK();
	}
	
}

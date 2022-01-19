/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;	//回复的ACK报文段
	int sequence=1;//用于记录当前待接收的包序号，注意包序号不完全是
	int last_sequence = -1; // 用于记录上一次收到包的序号

	/*构造函数*/
	public TCP_Receiver() {
		super();	//调用超类构造函数
		super.initTCP_Receiver(this);	//初始化TCP接收端
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		//检查校验码，生成ACK
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {  // 计算并比对校验和，如果相等：
			//生成ACK报文段（设置确认号）
			tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());  // 设置确认号为收到的TCP分组的seq
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());  // 新建一个TCP分组（ACK），发往发送方
			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));  // 设置ACK的校验位

			reply(ackPack);  // 回复ACK报文段

			int currentSequence = (recvPack.getTcpH().getTh_seq() - 1) / 100;  // 当前包的seq
			if (currentSequence != this.last_sequence) {  // 收到的包有新的seq
				this.last_sequence = currentSequence;  // 更新上一次接受的seq为本次接受到的包的seq

				// 将接收到的正确有序的数据插入 data 队列，准备交付
				this.dataQueue.add(recvPack.getTcpS().getData());
				sequence++;
			}
		}else{
			System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));  // 显示对于收到的分组的校验和计算结果
			System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());  // 显示收到的分组中的校验和
			System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
			tcpH.setTh_ack(-1); // -1表示这是一个NACK
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());  // 打一个NACK包
			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));  // 计算校验和

			reply(ackPack);  // 回复NACK报文段
		}
		
		System.out.println();
		
		
		//交付数据（每20组数据交付一次）
		if(dataQueue.size() == 20) 
			deliver_data();	
	}

	@Override
	//交付数据（将数据写入文件）；不需要修改
	public void deliver_data() {
		//检查dataQueue，将数据写入文件
		File fw = new File("recvData.txt");
		BufferedWriter writer;
		
		try {
			writer = new BufferedWriter(new FileWriter(fw, true));
			
			//循环检查data队列中是否有新交付数据
			while(!dataQueue.isEmpty()) {
				int[] data = dataQueue.poll();
				
				//将数据写入文件
				for(int i = 0; i < data.length; i++) {
					writer.write(data[i] + "\n");
				}
				
				writer.flush();		//清空输出缓存
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	//回复ACK报文段
	public void reply(TCP_PACKET replyPack) {
		// 设置错误控制标志
		// 0: 信道无差错
		// 1: 只出错
		// 2: 只丢包
		// 3: 只延迟
		// 4: 出错 / 丢包
		// 5: 出错 / 延迟
		// 6: 丢包 / 延迟
		// 7: 出错 / 丢包 / 延迟
		tcpH.setTh_eflag((byte)1);	//eFlag=1，信道只出错
				
		//发送数据报
		client.send(replyPack);
	}
	
}

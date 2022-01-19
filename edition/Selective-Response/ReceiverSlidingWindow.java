package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ReceiverSlidingWindow {
    private Client client;
    private int size = 16;
    private int base = 0;
    private TCP_PACKET[] packets = new TCP_PACKET[size];
    Queue<int[]> dataQueue = new LinkedBlockingQueue();
    private int counts = 0;

    public ReceiverSlidingWindow(Client client) {
        this.client = client;
    }

    public int receivePacket(TCP_PACKET packet) {
        int currentSequence = (packet.getTcpH().getTh_seq() - 1) / 100;  // 计算当前收到的分组序号

        if (currentSequence < base) {  // [rcvbase-N, rcvbase-1]
            if (base - size <= currentSequence && currentSequence <= base - 1) {
                return currentSequence;  // 对于失序分组也要返回ACK
            }
        } else if (currentSequence <= base + size - 1) {  // [rcvbase-N, rcvbase+N-1]
            packets[currentSequence - base] = packet;  // 对于正确分组，加入窗口中.

            if (currentSequence == base) {  // 接受到的分组位于窗口左沿

                // 滑动窗口
                slid();

                // 交付数据
                if (dataQueue.size() >= 20 || base == 1000) {
                    deliver_data();
                }

            }

            return currentSequence;  // 返回ACK
        }

        return -1;  // 错误返回-1
    }

    private void slid() {
        int leftMoveIndex = 0;  // 用于记录窗口左移到的位置：最小未收到数据包处
        while (leftMoveIndex <= size - 1 && packets[leftMoveIndex] != null) {
            leftMoveIndex ++;
        }

        for (int i = 0; i < leftMoveIndex; i++) {  // 将已接收到的分组加入交付队列
            dataQueue.add(packets[i].getTcpS().getData());
        }

        for (int i = 0; leftMoveIndex + i < size; i++) {  // 剩余位置的包左移
            packets[i] = packets[leftMoveIndex + i];
        }

        for (int i = size - (leftMoveIndex); i < size; i++) {  // 将左移的包原来位置处置空
            packets[i] = null;
        }

        base += leftMoveIndex;  // 移动窗口左沿
    }


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

}

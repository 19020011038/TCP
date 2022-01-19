package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Timer;

public class SenderSlidingWindow {
    private Client client;  // 客户端
    private int size = 16;  // 窗口大小
    private int base = 0;  // 窗口左值
    private int nextIndex = 0;  // 下一个包的存放位置
    private TCP_PACKET[] packets = new TCP_PACKET[size];  // 存储窗口内的包

    private Timer timer;  // 计时器
    private TaskPacketsRetransmit task;  // 重传任务

   /*构造函数*/
    public SenderSlidingWindow(Client client) {
        this.client = client;
    }

    /*判断窗口是否已满*/
    public boolean isFull() {
        return size <= nextIndex;
    }

    /*向窗口中加入包*/
    public void putPacket(TCP_PACKET packet) {
        packets[nextIndex] = packet;  // 在窗口的插入位置放入包
        if (nextIndex == 0) {  // 如果在窗口左沿，则要开启计时器
            timer = new Timer();
            task = new TaskPacketsRetransmit(client, packets);
            timer.schedule(task, 1000, 1000);
        }

        nextIndex++;  // 更新窗口的插入位置
    }

    /*接收到ACK*/
    public void receiveACK(int currentSequence) {
        if (base <= currentSequence && currentSequence < base + size) {  // 如果收到的ACK在窗口范围内
            for (int i = 0; currentSequence - base + 1 + i < size; i++) {  // 将窗口中位于确认的包之后的包整体移动到窗口左沿
                packets[i] = packets[currentSequence - base + 1 + i];
                packets[currentSequence - base + 1 + i] = null;
            }

            nextIndex -=currentSequence - base + 1;  // 更新nextIndex
            base = currentSequence + 1;  // 更新窗口左沿指示的seq

            timer.cancel();  // 停止计时器

            if (nextIndex != 0) {  // 窗口中仍有包，则重开计时器
                timer = new Timer();
                task = new TaskPacketsRetransmit(client, packets);
                timer.schedule(task, 1000, 1000);
            }

        }
    }
}

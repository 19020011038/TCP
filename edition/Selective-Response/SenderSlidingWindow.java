package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Timer;

public class SenderSlidingWindow {
    private Client client;  // 客户端
    private int size = 16;  // 窗口大小
    private int base = 0;  // 窗口左值
    private int nextIndex = 0;  // 下一个包的存放位置
    private TCP_PACKET[] packets = new TCP_PACKET[size];  // 存储窗口内的包
    private UDT_Timer[] timers = new UDT_Timer[size];  // 存储计时器



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
        timers[nextIndex] = new UDT_Timer();  // 为新放入窗口内的包增加计时器
        timers[nextIndex].schedule(new UDT_RetransTask(client, packet), 1000, 1000);
        nextIndex++;  // 更新窗口的插入位置
    }

    /*接收到ACK*/
    public void receiveACK(int currentSequence) {
        if (base <= currentSequence && currentSequence < base + size) {  // 接收到的包的序号位于窗口内
            if (timers[currentSequence - base] == null) {  // 表示接收到重复ACK，什么也不做
                return;
            }

            timers[currentSequence - base].cancel();  // 终止计时器
            timers[currentSequence - base] = null;  // 删除计时器

            if (currentSequence == base) {  // 接收到的ACK位于窗口左沿，则要移动窗口
                int leftMoveIndex = 0;  // 窗口左沿应该移动到的位置：最小未ACK的分组
                while (leftMoveIndex + 1 <= nextIndex && timers[leftMoveIndex] == null) {
                    leftMoveIndex ++;
                }


                for (int i = 0; leftMoveIndex + i < size; i++) {  // 将窗口内的包左移
                    packets[i] = packets[leftMoveIndex + i];
                    timers[i] = timers[leftMoveIndex + i];
                }

                for (int i = size - (leftMoveIndex); i < size; i++) {  // 清空已左移的包原来所在位置处的包和计时器
                    packets[i] = null;
                    timers[i] = null;
                }

                base += leftMoveIndex;  // 移动窗口左沿至leftMoveIndex处
                nextIndex -= leftMoveIndex;  // 移动下一个插入包的位置
            }
        }
    }
}

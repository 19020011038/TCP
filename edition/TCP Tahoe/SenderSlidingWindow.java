package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

public class SenderSlidingWindow {
    private Client client;  // 客户端
    private int size = 16;  // 窗口大小
    private int base = 0;  // 窗口左值
    private int nextIndex = 0;  // 下一个包的存放位置



    public int cwnd = 1;  // 拥塞窗口，初始值为1
    private volatile int ssthresh = 16;  // 慢开始门限，初始值为16
    private int CongestionAvoidanceCount = 0;  // 记录进入拥塞避免状态时收到的ACK数
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>();  // 存储发方窗口的包
    private Timer timer;  // 计时器
    private int lastACKSequence = -1;  // 上一次收到的ACK的包的seq
    private int lastACKSequenceCount = 0;  // 收到重复ACK的次数

    private int RENO_FLAG = -1;

    private int[] cwndChangeList = new int[1000];  // 用于记录 cwnd 变化
    private int cwndChangeListIndex = 0; // 记录数组下标
    private int[] ssthreshChangeList = new int[1000];  // 用于记录 ssthresh 变化
    private int ssthreshChangeListIndex = 0;  // 记录数组下标
    private int[] ackChangeList = new int[1000]; // 用于记录收到ack的序列号的变化
    private int ackChangeListIndex = 0;  // 记录数组下标
    private int lastCwnd = 1;  // 用于记录上一次的cwnd
    private int lastSsthresh = 16;  // y用于记录上一次的ssthresh



    public int getSsthresh() {return ssthresh;}

    public void setSsthresh(int ssthresh) {this.ssthresh = ssthresh;}

    public int getCwnd() {return cwnd;}

    public void setCwnd(int cwnd) {this.cwnd = cwnd;}

    public Hashtable<Integer, TCP_PACKET> getPackets() {return packets;}

    public int getLastACKSequence() {return lastACKSequence;}

    public void setCongestionAvoidanceCount(int CongestionAvoidanceCount) {this.CongestionAvoidanceCount = CongestionAvoidanceCount;}

   /*构造函数*/
    public SenderSlidingWindow(Client client) {
        this.client = client;
    }

    /*判断窗口是否已满*/
    public boolean isFull() {
        return cwnd <= packets.size();
    }

    /*向窗口中加入包*/
    public void putPacket(TCP_PACKET packet) {
        int currentSequence = ((packet.getTcpH().getTh_seq() - 1) / 100);

        if (packets.isEmpty()) {
            // 窗口左沿则开始计时器
            timer = new Timer();
            timer.schedule(new RetransmitTask(client, this), 1000, 1000);
        }
        packets.put(currentSequence, packet);

        // 如果是最后一个分组，则输出记录数组
        if (currentSequence == 999) {
            System.out.println("***** Change List Show *****");
            System.out.println(Arrays.toString(cwndChangeList));
            System.out.println(Arrays.toString(ssthreshChangeList));
            System.out.println(Arrays.toString(ackChangeList));
            System.out.println("***** Show End *****");
        } else if (currentSequence == 0) {
            cwndChangeList[cwndChangeListIndex ++] = cwnd;
            ssthreshChangeList[ssthreshChangeListIndex ++] = ssthresh;
            ackChangeList[ackChangeListIndex ++] = currentSequence;
        }
    }

    /*在 cwnd ssthresh 和 ack 变化列表中加入新值*/
    public void appendChange(int currentSequence) {
        cwndChangeList[cwndChangeListIndex ++] = cwnd;
        ssthreshChangeList[ssthreshChangeListIndex ++] = ssthresh;
        ackChangeList[ackChangeListIndex ++] = currentSequence;
    }

    /*接收到ACK*/
    public void receiveACK(int currentSequence) {


        // 收到重复ACK
        if (currentSequence == lastACKSequence) {
            lastACKSequenceCount++;
            if (lastACKSequenceCount == 4) {  // 三个重复ACK，执行快重传
                if (packets.containsKey(currentSequence + 1)) {
                    // 快重传
                    System.out.println("***** Fast Retransmit *****");
                    TCP_PACKET packet = packets.get(currentSequence + 1);
                    client.send(packet);
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(new RetransmitTask(client, this), 1000, 1000);
                }

                // 快恢复
                if (RENO_FLAG == 1) {
                    /*TCP Reno版本*/
                    System.out.println("***** Fast Recovery *****");
                    if (cwnd / 2 < 2) {
                        System.out.println("ssthresh: " + ssthresh + " ---> 2");
                        ssthresh = 2;  // ssthresh 不得小于2
                    } else {
                        System.out.println("ssthresh: " + ssthresh + " ---> " + cwnd / 2);
                        ssthresh = cwnd / 2;  // 慢开始门限变为 cwnd

                    }
                    System.out.println("cwnd: " + cwnd + " ---> " + ssthresh);
                    cwnd = ssthresh;  // cwnd 置为ssthresh
                    CongestionAvoidanceCount = 0;
                } else {
                    /*TCP Tahoe版本*/
                    if (cwnd / 2 < 2) {
                        System.out.println("ssthresh: " + ssthresh + " ---> 2");
                        ssthresh = 2;  // ssthresh 不得小于2
                    } else {
                        System.out.println("ssthresh: " + ssthresh + " ---> " + cwnd / 2);
                        ssthresh = cwnd / 2;  // 慢开始门限变为 cwnd 的一半

                    }
                    System.out.println("cwnd: " + cwnd + " ---> 1");
                    cwnd = 1;  // cwnd 置为1
                    CongestionAvoidanceCount = 0;
                }
                appendChange(currentSequence);
            }
        } else {

            // 清空计时器
            timer.cancel();

            // 收到新的ACK
            for (int i = lastACKSequence + 1; i <= currentSequence; i++) {  // 清除前面的包
                packets.remove(i);
            }

            lastACKSequence = currentSequence;  // 重置lastACKSequence为当前收到ACK的包的seq
            lastACKSequenceCount = 1;  // 重置lastACKCount为1

            // 如果窗口中仍有分组，则重开计时器
            if (!packets.isEmpty()) {
                timer = new Timer();
                timer.schedule(new RetransmitTask(client, this), 1000, 1000);
            }

            if (cwnd < ssthresh) {
                // 慢启动
                System.out.println("***** Slow Start *****");
                System.out.println("cwnd: " + cwnd + " ---> " + (cwnd + 1));
                cwnd ++;  // 每收到一个ACK， cwnd加一
                appendChange(currentSequence);
            } else {
                // 拥塞避免
                CongestionAvoidanceCount ++;
                System.out.println("***** Congestion Avoidance *****");
                System.out.println("cwnd: " + cwnd + " NO. " + CongestionAvoidanceCount);
                if (CongestionAvoidanceCount == cwnd) {  // 收到ACK数量达到 cwnd 大小时
                    CongestionAvoidanceCount = 0;  // 重置计数器
                    System.out.println("cwnd: " + cwnd + " ---> " + (cwnd + 1));
                    cwnd ++;  // cwnd 加一
                    appendChange(currentSequence);
                }
            }


        }


    }
}

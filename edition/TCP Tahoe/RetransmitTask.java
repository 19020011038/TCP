package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

public class RetransmitTask extends TimerTask {
    private Client client;
    private SenderSlidingWindow window;


    public RetransmitTask(Client client, SenderSlidingWindow window) {
        this.client = client;
        this.window = window;
    }

    @Override
    public void run() {

        // 清空拥塞避免计数器
        window.setCongestionAvoidanceCount(0);


        // 立刻重传分组(窗口左沿）
        if ( window.getPackets().containsKey(window.getLastACKSequence() + 1)) {
            client.send(window.getPackets().get(window.getLastACKSequence() + 1));
        }

        // 超时重传
        System.out.println("***** Timeout Retransmit *****");
        if (window.getCwnd() / 2 < 2) {
            System.out.println("ssthresh: " + window.getSsthresh() + " ---> 2");
            window.setSsthresh(2);  // ssthresh 不得小于2
        } else {
            System.out.println("ssthresh: " + window.getSsthresh() + " ---> " + window.getCwnd() / 2);
            window.setSsthresh(window.getCwnd() / 2);  // 慢开始门限变为 cwnd 的一半
        }
        System.out.println("cwnd: " + window.getCwnd() + " ---> 1");
        window.setCwnd(1);  // cwnd 置为1

        window.appendChange(window.getLastACKSequence());

    }
}

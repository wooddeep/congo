package com.migu.sdk.protocol;

import com.migu.sdk.toolkit.NumberUtil;

/**
 * Created by lihan on 2018/6/21.
 */
public class DisMsg {
    private int numberReturned;

    public int getNumberReturned() {
        return numberReturned;
    }

    public void setNumberReturned(int numberReturned) {
        this.numberReturned = numberReturned;
    }

    public DisMsg(int nr) {
        this.numberReturned = nr;
    }

    public DisMsg(byte [] arr) {
        this.numberReturned = NumberUtil.byte4ToInt(arr, 0);
    }

    public byte [] getBytes() {
        byte [] out = NumberUtil.intToByte4(this.numberReturned);
        return out;
    }

    public int getBytesCnt() {
        return 4;
    }

}

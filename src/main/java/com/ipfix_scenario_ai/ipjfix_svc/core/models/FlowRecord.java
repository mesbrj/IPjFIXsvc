package com.ipfix_scenario_ai.ipjfix_svc.core.models;

import java.util.Date;

public class FlowRecord {
    private String id;
    private String sourceIP;
    private String destIP;
    private int sourcePort;
    private int destPort;
    private String protocol;
    private long bytes;
    private long packets;
    private long reverseBytes;
    private long reversePackets;
    private Date timestamp;
    private Date flowStartTime;
    private Date flowEndTime;
    private int tcpFlags;
    private int tosValue;

    public FlowRecord() {}
    public FlowRecord(String id, String sourceIP, String destIP,
                      int sourcePort, int destPort, String protocol,
                      long bytes, long packets, long reverseBytes,
                      long reversePackets, Date timestamp, Date flowStartTime,
                      Date flowEndTime, int tcpFlags, int tosValue, String deviceId) {
        this.id = id;
        this.sourceIP = sourceIP;
        this.destIP = destIP;
        this.sourcePort = sourcePort;
        this.destPort = destPort;
        this.protocol = protocol;
        this.bytes = bytes;
        this.packets = packets;
        this.reverseBytes = reverseBytes;
        this.reversePackets = reversePackets;
        this.timestamp = timestamp;
        this.flowStartTime = flowStartTime;
        this.flowEndTime = flowEndTime;
        this.tcpFlags = tcpFlags;
        this.tosValue = tosValue;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceIP() { return sourceIP; }
    public void setSourceIP(String sourceIP) { this.sourceIP = sourceIP; }
    
    public String getDestIP() { return destIP; }
    public void setDestIP(String destIP) { this.destIP = destIP; }

    public int getSourcePort() { return sourcePort; }
    public void setSourcePort(int sourcePort) { this.sourcePort = sourcePort; }

    public int getDestPort() { return destPort; }
    public void setDestPort(int destPort) { this.destPort = destPort; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public long getBytes() { return bytes; }
    public void setBytes(long bytes) { this.bytes = bytes; }

    public long getPackets() { return packets; }
    public void setPackets(long packets) { this.packets = packets; }

    public long getReverseBytes() { return reverseBytes; }
    public void setReverseBytes(long reverseBytes) { this.reverseBytes = reverseBytes; }

    public long getReversePackets() { return reversePackets; }
    public void setReversePackets(long reversePackets) { this.reversePackets = reversePackets; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public Date getFlowStartTime() { return flowStartTime; }
    public void setFlowStartTime(Date flowStartTime) { this.flowStartTime = flowStartTime; }

    public Date getFlowEndTime() { return flowEndTime; }
    public void setFlowEndTime(Date flowEndTime) { this.flowEndTime = flowEndTime; }

    public int getTcpFlags() { return tcpFlags; }
    public void setTcpFlags(int tcpFlags) { this.tcpFlags = tcpFlags; }

    public int getTosValue() { return tosValue; }
    public void setTosValue(int tosValue) { this.tosValue = tosValue; }

    public long getTotalBytes() {
        return bytes + reverseBytes;
    }

    public long getTotalPackets() {
        return packets + reversePackets;
    }

    public boolean isBidirectional() {
        return reverseBytes > 0 || reversePackets > 0;
    }

    @Override
    public String toString() {
        return String.format("FlowRecord{id='%s', %s:%d -> %s:%d, protocol='%s', bytes=%d, packets=%d}", 
                            id, sourceIP, sourcePort, destIP, destPort, protocol, bytes, packets);
    }
}
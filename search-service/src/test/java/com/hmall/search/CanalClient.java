package com.hmall.search;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.CanalEntry.*;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class CanalClient {

    //sql队列
    private Queue<String> SQL_QUEUE = new ConcurrentLinkedQueue<>();

    @Resource
    private DataSource dataSource;

    /**
     * canal入库方法
     */
    @Test
    public void run() throws InterruptedException, InvalidProtocolBufferException {

        CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress("192.168.175.129",
                11111), "example", "", "");
        int batchSize = 10;
        int emptyCount = 0;
        while(true){
            connector.connect();
            connector.subscribe("hm-item.*");
            Message message = connector.get(batchSize);
            List<Entry> entries = message.getEntries();

            if(entries.size() <= 0){
//                System.out.println("当次抓取没有数据");
                Thread.sleep(10000);
            }else {
                for (Entry entry : entries) {
                    //获取表名
                    String tableName = entry.getHeader().getTableName();
                    //获取类型
                    EntryType entryType = entry.getEntryType();
                    //获取序列化后的数据
                    ByteString storeValue = entry.getStoreValue();
                    //
                    if(EntryType.ROWDATA.equals(entryType)){
                        RowChange rowChange = RowChange.parseFrom(storeValue);
                        EventType eventType = rowChange.getEventType();
                        List<RowData> rowDatasList = rowChange.getRowDatasList();
                        for (RowData rowData : rowDatasList) {
                            JSONObject beforeData = new JSONObject();
                            List<Column> beforeColumnsList = rowData.getBeforeColumnsList();
                            for (Column column : beforeColumnsList) {
                                beforeData.put(column.getName(), column.getValue());
                            }
                            JSONObject afterData = new JSONObject();
                            List<Column> afterColumnsList = rowData.getAfterColumnsList();
                            for (Column column : afterColumnsList) {
                                afterData.put(column.getName(), column.getValue());
                            }

                            System.out.println("===============================");
                            System.out.println("table:" + tableName+",eventType:"+eventType);
                            System.out.println("beforeData:"+beforeData);
                            System.out.println("afterData:"+afterData);
                            System.out.println("================================");
                        }

                    }

                }
            }
        }



    }


}

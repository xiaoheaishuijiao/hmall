package com.hmall.item.constants;

public interface MQConstants {
    String ITEM_EXCHANGE_NAME = "item.update.direct";
    String ITEM_UPDATE_QUEUE_NAME = "item.update.queue";
    String ITEM_UPDATE_ALL_QUEUE_NAME = "item.update.all.queue";
    String ITEM_UPDATE_STATUS_QUEUE_NAME = "item.update.status.queue";
    String ITEM_DELETE_QUEUE_NAME = "item.delete.queue";
    String ITEM_UPDATE_KEY = "update.item.to.elasticsearch";

    String ITEM_STATUS_UPDATE_KEY = "update.item.status.to.elasticsearch";
    String ITEM_ALL_UPDATE_KEY = "update.item.all.to.elasticsearch";
    String ITEM_DELETE_KEY = "delete.item.from.elasticsearch";
}

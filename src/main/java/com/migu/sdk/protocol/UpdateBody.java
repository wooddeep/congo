package com.migu.sdk.protocol;

import org.bson.RawBsonDocument;

/**
 * Created by lihan on 2018/7/9.
 */
public class UpdateBody extends MongoBody  {
    public int zero;
    public String fullCollectionName;
    public int flags;
    public RawBsonDocument selector;
    public RawBsonDocument update;
}

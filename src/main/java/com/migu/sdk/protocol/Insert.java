package com.migu.sdk.protocol;

import org.bson.RawBsonDocument;

/**
 * Created by lihan on 2018/6/27.
 */
public class Insert extends MongoBody {
    public int flags;
    public String fullCollectionName;
    public RawBsonDocument document;
}

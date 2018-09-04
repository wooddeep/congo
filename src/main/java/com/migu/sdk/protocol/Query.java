package com.migu.sdk.protocol;

import org.bson.RawBsonDocument;

/**
 * Created by lihan on 2018/6/15.
 */
public class Query extends MongoBody {
    public int flags;//int32  // bit vector of query options.  See below for details.
    public String fullCollectionName;//string // "dbname.collectionname"
    public int numberToSkip;//int32  // number of documents to skip
    public int numberToReturn;//int32  // number of documents to return
    public RawBsonDocument query;//bson.D // query object.  See below for details.
    public RawBsonDocument returnFieldsSelector;//bson.D // Optional. Selector indicating the fields

}

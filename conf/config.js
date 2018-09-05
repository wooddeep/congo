/**
 * Created by lihan on 2018/1/3.
 */

/*
var Buffer = require("vertx-js/buffer");
var sd = vertx.sharedData();
var config = sd.getLocalMap("config");
*/

var dbgFlag = true

var onlineConf = { // 线上环境配置
    "dbgFlag": dbgFlag,
    "logdir" : "/work/logs", // 日志路径
    "master": "false",
    "mongod": "127.0.0.1:27017"
}


var dbgConf = {
    "dbgFlag": dbgFlag,
    "logdir" : "/work/logs", // 日志路径
    "master": "false",
    "mongod": "127.0.0.1:27017"
}

var getConfig = function() {
    if (dbgFlag) {
        return JSON.stringify(dbgConf)
    } else {
        return JSON.stringify(onlineConf)
    }
}

module.exports = {
  "config": dbgFlag ? dbgConf : onlineConf
}
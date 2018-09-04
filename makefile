# 
# Author: lihan@migu.cn
# History: create at 2018-02-06
# Description: generate the version infomation of the jar file!
#

# !/bin/make

## get system date and time
BUILD_TIME=$(shell awk 'BEGIN{print strftime("%Y-%m-%d %T", systime())}')

JAVA_FILE=./src/main/java/com/migu/sdk/update/Version.java

package: vergen
	mvn package	

compile: vergen
	mvn compile

clean: 
	mvn clean

vergen:
	@echo $(BUILD_TIME)
	@sed -i 's/[0-9]\{4\}.[0-9]\{2\}.[0-9]\{2\}.[0-9]\{2\}.[0-9]\{2\}.[0-9]\{2\}/$(BUILD_TIME)/' $(JAVA_FILE)


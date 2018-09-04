/**
 * Copyright (c) 2016-2020, 李翰 (niutourenqz@sina.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.migu.sdk.framework.annotate;

import com.migu.sdk.framework.SubCmdItf;
import com.migu.sdk.protocol.subcmd.SubCmd;

import java.lang.reflect.Field;
import java.util.Set;

public class FrameCore {

    public static void prevHandle(Set<Class<?>> classSets) throws Exception {
        for (Class cls : classSets) {
            String className = cls.getName();
            if (className.contains("$"))
                continue;

            for (Field field : cls.getFields()) {
                if (field.isAnnotationPresent(SubCommand.class)) {
                    String name = field.getAnnotation(SubCommand.class).name();
                    if (!name.isEmpty()) {
                        SubCmdItf cmd = (SubCmdItf) field.get(SubCmdItf.class);
                        SubCmd.regSubCmd(name, cmd);
                    }
                }
            }
        }
    }

}

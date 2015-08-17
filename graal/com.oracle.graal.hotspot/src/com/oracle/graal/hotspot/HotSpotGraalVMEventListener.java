/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.hotspot;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.debug.*;
import com.oracle.graal.hotspot.logging.*;

import jdk.internal.jvmci.code.*;
import jdk.internal.jvmci.hotspot.*;
import jdk.internal.jvmci.service.*;

@ServiceProvider(HotSpotVMEventListener.class)
public class HotSpotGraalVMEventListener implements HotSpotVMEventListener {

    @Override
    public void notifyShutdown() {
        HotSpotGraalRuntime.runtime().shutdown();
    }

    @Override
    public void notifyInstall(HotSpotCodeCacheProvider codeCache, InstalledCode installedCode, CompilationResult compResult) {
        if (Debug.isDumpEnabled()) {
            Debug.dump(new Object[]{compResult, installedCode}, "After code installation");
        }
        if (Debug.isLogEnabled()) {
            Debug.log("%s", codeCache.disassemble(installedCode));
        }
    }

    @Override
    public CompilerToVM completeInitialization(HotSpotJVMCIRuntime runtime, CompilerToVM compilerToVM) {
        CompilerToVM toVM = compilerToVM;
        if (CountingProxy.ENABLED) {
            toVM = CountingProxy.getProxy(CompilerToVM.class, toVM);
        }
        if (Logger.ENABLED) {
            toVM = LoggingProxy.getProxy(CompilerToVM.class, toVM);
        }

        if (Boolean.valueOf(System.getProperty("jvmci.printconfig"))) {
            printConfig(runtime.getConfig());
        }

        return toVM;
    }

    private static void printConfig(HotSpotVMConfig config) {
        Field[] fields = config.getClass().getDeclaredFields();
        Map<String, Field> sortedFields = new TreeMap<>();
        for (Field f : fields) {
            f.setAccessible(true);
            sortedFields.put(f.getName(), f);
        }
        for (Field f : sortedFields.values()) {
            try {
                Logger.info(String.format("%9s %-40s = %s", f.getType().getSimpleName(), f.getName(), Logger.pretty(f.get(config))));
            } catch (Exception e) {
            }
        }
    }
}

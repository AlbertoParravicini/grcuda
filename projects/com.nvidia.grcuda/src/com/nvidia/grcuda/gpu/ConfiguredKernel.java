/*
 * Copyright (c) 2019, NVIDIA CORPORATION. All rights reserved.
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of NVIDIA CORPORATION nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nvidia.grcuda.gpu;

import com.nvidia.grcuda.gpu.computation.KernelExecution;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class ConfiguredKernel extends ProfilableElement implements TruffleObject {

    private final Kernel kernel;

    private final KernelConfig config;

    public ConfiguredKernel(Kernel kernel, KernelConfig config) {
        // ConfiguredKernel is a Profilable Element
        super(true);
        this.kernel = kernel;
        this.config = config;
    }



    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    Object execute(Object[] arguments,
                    @CachedLibrary(limit = "3") InteropLibrary boolAccess,
                    @CachedLibrary(limit = "3") InteropLibrary int8Access,
                    @CachedLibrary(limit = "3") InteropLibrary int16Access,
                    @CachedLibrary(limit = "3") InteropLibrary int32Access,
                    @CachedLibrary(limit = "3") InteropLibrary int64Access,
                    @CachedLibrary(limit = "3") InteropLibrary doubleAccess) throws UnsupportedTypeException, ArityException {
        kernel.incrementLaunchCount();
        try (KernelArguments args = kernel.createKernelArguments(arguments, boolAccess, int8Access, int16Access,
                        int32Access, int64Access, doubleAccess)) {
            // If using a manually specified stream, do not schedule it automatically, but execute it immediately;
            if (!config.useCustomStream()) {
                new KernelExecution(this, args).schedule();
                
            } else {
                kernel.getGrCUDAExecutionContext().getCudaRuntime().cuLaunchKernel(kernel, config, args, config.getStream());

            }
        }
        return this;
    }

    public Kernel getKernel() {
        return kernel;
    }

    public KernelConfig getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return "ConfiguredKernel(" + kernel.toString() + "; " + config.toString() + ")";
    }
}

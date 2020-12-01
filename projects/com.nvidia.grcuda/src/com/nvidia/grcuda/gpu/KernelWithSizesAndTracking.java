package com.nvidia.grcuda.gpu;

import com.nvidia.grcuda.Type;
import com.nvidia.grcuda.array.DeviceArray;
import com.nvidia.grcuda.gpu.executioncontext.AbstractGrCUDAExecutionContext;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

public final class KernelWithSizesAndTracking extends KernelWithSizes {

    protected final DeviceArray trackingArray;

    public KernelWithSizesAndTracking(AbstractGrCUDAExecutionContext grCUDAExecutionContext, String kernelName, String kernelSymbol,
                                      long kernelFunction, String kernelSignature, CUDARuntime.CUModule module, int numOfPointers) {
        super(grCUDAExecutionContext, kernelName, kernelSymbol, kernelFunction, kernelSignature, module, numOfPointers);
        this.trackingArray = new DeviceArray(grCUDAExecutionContext, numOfPointers, Type.SINT64);
    }

    public KernelWithSizesAndTracking(AbstractGrCUDAExecutionContext grCUDAExecutionContext, String kernelName, String kernelSymbol,
                           long kernelFunction, String kernelSignature, CUDARuntime.CUModule module, String ptx, int numOfPointers) {
        super(grCUDAExecutionContext, kernelName, kernelSymbol, kernelFunction, kernelSignature, module, ptx, numOfPointers);
        this.trackingArray = new DeviceArray(grCUDAExecutionContext, numOfPointers, Type.SINT64);
    }

    @Override
    protected KernelArguments createKernelArguments(Object[] args, InteropLibrary booleanAccess,
                                                    InteropLibrary int8Access, InteropLibrary int16Access,
                                                    InteropLibrary int32Access, InteropLibrary int64Access, InteropLibrary doubleAccess)
            throws UnsupportedTypeException, ArityException {
        // The number of provided arguments should be equal to the number of arguments in the modified signature - 2,
        // as we added the sizes array and the tracking array to the modified signature;
        if (args.length != this.kernelComputationArguments.length - 2) {
            CompilerDirectives.transferToInterpreter();
            throw ArityException.create(this.kernelComputationArguments.length, args.length + 2);
        }
        // Initialize the arguments, and create a new array that stores the sizes of each array;
        KernelArguments kernelArgs = new KernelArgumentsWithSizesAndTracking(args, this.kernelComputationArguments, sizesArray, trackingArray);

        // Add all the arguments provided by the user;
        for (int argIdx = 0; argIdx < args.length; argIdx++) {
            processAndAddKernelArgument(kernelArgs, args, argIdx, booleanAccess, int8Access, int16Access, int32Access, int64Access, doubleAccess);
        }
        return kernelArgs;
    }
}

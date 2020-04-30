package com.nvidia.grcuda.gpu.computation;

import com.nvidia.grcuda.gpu.ArgumentType;

import java.util.Objects;

/**
 * Defines a {@link GrCUDAComputationalElement} argument representing the elements of a NFI signature.
 * For each argument, store its type, if it's a pointer,
 * and if it's constant (i.e. its content cannot be modified in the computation).
 * This class also holds a reference to the actual object associated to the argument;
 */
public class ComputationArgumentWithValue extends ComputationArgument {
    private final Object argumentValue;

    public ComputationArgumentWithValue(ArgumentType type, boolean isArray, boolean isConst, Object argumentValue) {
        super(type, isArray, isConst);
        this.argumentValue = argumentValue;
    }

    public ComputationArgumentWithValue(ComputationArgument computationArgument, Object argumentValue) {
        super(computationArgument.getType(), computationArgument.isArray(), computationArgument.isConst());
        this.argumentValue = argumentValue;
    }

    public Object getArgumentValue() { return this.argumentValue; }

    @Override
    public String toString() {
        return "ComputationArgumentWithValue(" +
                "argumentValue=" + argumentValue +
                ", type=" + type +
                ", isArray=" + isArray +
                ", isConst=" + isConst +
                ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComputationArgumentWithValue that = (ComputationArgumentWithValue) o;
        return Objects.equals(argumentValue, that.argumentValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(argumentValue);
    }
}
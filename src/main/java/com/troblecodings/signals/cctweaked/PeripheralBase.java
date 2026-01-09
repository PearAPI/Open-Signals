package com.troblecodings.signals.cctweaked;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.troblecodings.signals.OpenSignalsMain;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

public abstract class PeripheralBase<T> implements IPeripheral {

    public final T entity;
    List<Method> methods;

    public PeripheralBase(final T entity) {
        this.entity = entity;

        methods = scanMethods();
    }

    List<Method> scanMethods() {
        List<Method> methods = new ArrayList<>();
        for (final Method method : this.getClass().getMethods()) {
            if (method.isAnnotationPresent(LuaMethod.class)) {
                methods.add(method);
            }
        }
        return methods;
    }

    @Override
    public String[] getMethodNames() {
        return methods.stream().map(Method::getName).toArray(String[]::new);
    }

    @Override
    public Object[] callMethod(@Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int methodIndex,
            @Nonnull Object[] arguments) throws LuaException, InterruptedException {
        if (methodIndex < 0 || methodIndex >= methods.size())
            throw new LuaException("Method index out of bounds");

        Method method = methods.get(methodIndex);
        int parameterCount = method.getParameterCount();
        Class<?>[] parametertypes = method.getParameterTypes();

        if (arguments.length != parameterCount) {
            throw new LuaException("Invalid argument length! Expected " + parameterCount + " arguments, but got "
                    + arguments.length);
        }

        // check if arument types can convert to parameter types
        for (int i = 0; i < arguments.length; i++) {
            if (!canConvert(arguments[i], parametertypes[i]).isPresent()) {
                throw new LuaException("Invalid argument Type!\n" + parametertypes[i].getName() + " expected, got "
                        + arguments[i].getClass().getName() + " !");
            }
        }

        Object[] convertedArguments = new Object[arguments.length];
        // convert arguments to parameter types
        for (int i = 0; i < arguments.length; i++) {
            convertedArguments[i] = canConvert(arguments[i], parametertypes[i]).get();
        }

        try {
            return new Object[] { method.invoke(this, convertedArguments) };
        } catch (Exception e) {
            throw new LuaException("Error Executing Method");
        }
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other;
    }

    @Override
    public String getType() {
        return "computer_link";
    }

    private <T> Optional<T> canConvert(Object value, Class<T> type) {
        if (value == null)
            return Optional.empty();

        boolean isNumber = value.toString().matches("-?\\d+(\\.\\d+)?");

        if (isNumber) {
            boolean isDecimal = value.toString().contains(".");

            if (type == int.class)
                return Optional.of(type.cast(Integer.parseInt(value.toString())));

            if (type == long.class)
                return Optional.of(type.cast(Long.parseLong(value.toString())));

            if (type == float.class)
                return Optional.of(type.cast(Float.parseFloat(value.toString())));

            if (type == double.class)
                return Optional.of(type.cast(Double.parseDouble(value.toString())));
        }

        return Optional.of(type.cast(value));
    }

    protected Optional<String> parseString(Object object) {
        return Optional.ofNullable(object.toString());
    }
}

package com.troblecodings.signals.cctweaked;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        // for (int i = 0; i < arguments.length; i++) {
        // if (!canConvert(arguments[i], parametertypes[i])) {
        // throw new LuaException("Invalid argument Type!\n" +
        // parametertypes[i].getName() + " expected, got "
        // + arguments[i].getClass().getName() + " !");
        // }
        // }

        Object[] convertedArguments = new Object[arguments.length];
        // convert arguments to parameter types
        for (int i = 0; i < arguments.length; i++) {
            convertedArguments[i] = canConvert(arguments[i], parametertypes[i]);
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

    private <T> T canConvert(Object value, Class<T> type) throws LuaException {
        if (value == null)
            throw new LuaException("Value cannot be null / nil");

        if (!type.isPrimitive() && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        if (type == String.class) {
            return (T) value.toString();
        }

        if (type == boolean.class || type == Boolean.class) {
            if (value instanceof Boolean)
                return (T) value;

            String val = value.toString().toLowerCase();

            if (val.equals("true"))
                return (T) Boolean.TRUE;

            if (val.equals("false"))
                return (T) Boolean.FALSE;

            throw new LuaException("Value is not a Boolean!");
        }

        if (Number.class.isAssignableFrom(getWrapperClass(type))) {
            try {
                double numVal;

                if (value instanceof Number) {
                    numVal = ((Number) value).doubleValue();
                } else {
                    numVal = Double.parseDouble(value.toString());
                }

                if (type == int.class || type == Integer.class) {
                    return (T) Integer.valueOf((int) numVal);
                }
                if (type == long.class || type == Long.class) {
                    return (T) Long.valueOf((long) numVal);
                }
                if (type == float.class || type == Float.class) {
                    return (T) Float.valueOf((float) numVal);
                }
                if (type == double.class || type == Double.class) {
                    return (T) Double.valueOf(numVal);
                }
            } catch (NumberFormatException | ArithmeticException e) {
                OpenSignalsMain.getLogger().error("Error converting number", e);
                throw new LuaException("Value '" + value + "' is not a valid number");
            }
        }
        throw new LuaException("Unsupported conversion to type: " + type.getSimpleName());
    }

    private Class<?> getWrapperClass(Class<?> type) {
        if (type == int.class)
            return Integer.class;
        if (type == long.class)
            return Long.class;
        if (type == double.class)
            return Double.class;
        if (type == float.class)
            return Float.class;
        if (type == boolean.class)
            return Boolean.class;
        return type;
    }

    private final List<IComputerAccess> computers = new ArrayList<>();

    @Override
    public void attach(@Nonnull IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(@Nonnull IComputerAccess computer) {
        computers.remove(computer);
    }

    public void queueEvent(String event, Object... arguments) {
        for (IComputerAccess computer : computers) {
            computer.queueEvent(event, arguments);
        }
    }

    protected Optional<String> parseString(Object object) {
        return Optional.ofNullable(object.toString());
    }
}

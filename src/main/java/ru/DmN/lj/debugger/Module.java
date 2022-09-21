package ru.DmN.lj.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Module {
    public final String name;
    public final List<Module> sub = new ArrayList<>();
    public final Map<String, Object> variables = new HashMap<>();
    public final List<Method> methods = new ArrayList<>();
    public boolean initialized = false;

    public Module(String name) {
        this.name = name;
    }

    public void init(SimpleDebugger debugger) {
        var init = this.methods.stream().filter(m -> m.name.equals("init") && m.desc.equals("V")).findFirst();
        init.ifPresent(method -> debugger.run(this, method));
    }

    @Override
    public String toString() {
        return "[" + name + "]";
    }
}

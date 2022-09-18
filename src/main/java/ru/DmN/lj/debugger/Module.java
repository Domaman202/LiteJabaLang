package ru.DmN.lj.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Module {
    public final String name;
    public final Map<String, Object> variables = new HashMap<>();
    public final List<Method> methods = new ArrayList<>();
    public boolean initialized = false;

    public Module(String name) {
        this.name = name;
    }

    public void init(SimpleDebugger debugger) {
        debugger.run(this, this.methods.stream().filter(m -> m.name.equals("init") && m.desc.equals("V")).findFirst().orElseThrow(() -> new RuntimeException("Метод `init|V` не найден")));
    }
}

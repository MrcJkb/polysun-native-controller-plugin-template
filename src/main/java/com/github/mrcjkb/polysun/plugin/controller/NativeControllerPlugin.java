package com.github.mrcjkb.polysun.plugin.controller;

import java.util.List;
import java.util.Map;

import com.github.mrcjkb.polysun.plugin.controller.nativeplugincontroller.NativePluginController;
import com.velasolaris.plugin.controller.spi.AbstractControllerPlugin;
import com.velasolaris.plugin.controller.spi.IPluginController;

public class NativeControllerPlugin extends AbstractControllerPlugin {

    @Override
    public List<Class<? extends IPluginController>> getControllers(Map<String, Object> parameters) {
        return List.of(NativePluginController.class);
    }

    @Override
    public String getCreator() {
        return "Marc Jakobi";
    }

    @Override
    public String getDescription() {
        return "Controller plugin for use with native libraries";
    }

}

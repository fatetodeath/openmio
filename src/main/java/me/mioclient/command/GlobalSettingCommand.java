package me.mioclient.command;

import me.mioclient.api.ClientServices;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import java.util.Iterator;
import java.util.function.BiConsumer;
import me.mioclient.api.Setting;
import me.mioclient.module.Module;
import me.mioclient.module.client.Colors;
import me.mioclient.module.client.UI;
import net.minecraft.commands.CommandSourceStack;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;

public class GlobalSettingCommand extends CommandFeature {
    public GlobalSettingCommand() {
        super("global");
        setAliases("globalsync", "syncall");
    }

    @Override // me.mioclient.command.CommandFeature
    public void exec(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder) {
        literalArgumentBuilder.then(CommandFeature.literal("color").then(CommandFeature.argument("value", BoolArgumentType.bool()).executes(commandContext -> {
            forEachModuleSetting((module, setting) -> {
                if ((module instanceof Colors) || (module instanceof UI) || !(setting instanceof ColorSetting)) {
                    return;
                }
                ((ColorSetting) setting).setSyncEnabled(((Boolean) commandContext.getArgument("value", Boolean.class)).booleanValue());
            });
            return 1;
        })));
        literalArgumentBuilder.then(CommandFeature.literal("linewidth").then(CommandFeature.argument("value", FloatArgumentType.floatArg(0.1f, 5.0f)).executes(commandContext2 -> {
            float floatValue = ((Float) commandContext2.getArgument("value", Float.class)).floatValue();
            forEachModuleSetting((module, setting) -> {
                if ((setting.getValue() instanceof Float) && setting.getName().equalsIgnoreCase("LineWidth")) {
                    ((NumberSetting) setting).setValue(Float.valueOf(floatValue));
                }
            });
            return 1;
        })));
    }

    public void forEachModuleSetting(BiConsumer<Module, Setting<?>> biConsumer) {
        for (Module module : ClientServices.moduleRegistry.getRegistry()) {
            Iterator<Setting<?>> it = module.getRegistry().iterator();
            while (it.hasNext()) {
                biConsumer.accept(module, it.next());
            }
        }
    }
}

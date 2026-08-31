package run.wanchun.cooperation;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * 合作咨询插件主类。
 */
@Component
public class CooperationPlugin extends BasePlugin {

    public CooperationPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void start() {
        System.out.println("合作咨询插件启动成功！");
    }

    @Override
    public void stop() {
        System.out.println("合作咨询插件停止！");
    }
}

package run.wanchun.cooperation;

import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import run.wanchun.cooperation.extension.Cooperation;

/**
 * 合作咨询插件主类。
 */
@Component
public class CooperationPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public CooperationPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(Cooperation.class);
        System.out.println("合作咨询插件启动成功！ Scheme 已注册: " + Cooperation.class.getName());
    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(Cooperation.class));
        System.out.println("合作咨询插件停止！ Scheme 已注销");
    }
}

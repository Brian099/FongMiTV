package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.Decoder;
import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Rule;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.bean.Doh;
import com.github.catvod.bean.Header;
import com.github.catvod.bean.Proxy;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.google.gson.JsonObject;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VodConfig {

    private static final String TAG = VodConfig.class.getSimpleName();
    private final AtomicInteger taskId = new AtomicInteger(0);

    private Site home;
    private String wall;
    private Parse parse;
    private Config config;
    private List<Doh> doh;
    private List<Rule> rules;
    private List<Site> sites;
    private List<String> ads;
    private List<String> flags;
    private List<Parse> parses;
    private Future<?> future;

    // ★ 新增：启动严格校验开关
    private boolean strictBoot = false;
    private boolean bootChecked = false;

    private static class Loader {
        static volatile VodConfig INSTANCE = new VodConfig();
    }

    public static VodConfig get() {
        return Loader.INSTANCE;
    }

    public VodConfig strictBoot(boolean enable) {
        this.strictBoot = enable;
        return this;
    }

    public VodConfig init() {
        return config(Config.vod());
    }

    public VodConfig config(Config config) {
        this.config = config;
        return this;
    }

    public VodConfig clear() {
        home = null;
        wall = null;
        parse = null;
        sites = null;
        BaseLoader.get().clear();
        return this;
    }

    private boolean isCanceled(Throwable e) {
        return "Canceled".equals(e.getMessage()) ||
                e instanceof InterruptedException ||
                e instanceof InterruptedIOException;
    }

    public void load(Callback callback) {
        int id = taskId.incrementAndGet();
        if (future != null && !future.isDone()) future.cancel(true);
        future = App.submit(() -> loadConfig(id, config, callback));
        callback.start();
    }

    private void loadConfig(int id, Config config, Callback callback) {
        try {
            OkHttp.cancel(TAG);
            Server.get().start();

            String json = Decoder.getJson(UrlUtil.convert(config.getUrl()), TAG);
            if (TextUtils.isEmpty(json)) {
                throw new Exception("配置请求返回空内容");
            }

            JsonObject object;
            try {
                object = Json.parse(json).getAsJsonObject();
            } catch (Throwable e) {
                throw new Exception("配置 JSON 格式错误", e);
            }

            // ★ 只在【启动 + 内置 VOD + 第一次】做多仓严格校验
            boolean strictCheck = strictBoot && !bootChecked && config == Config.vod();

            if (strictCheck) {
                if (!object.has("urls")) {
                    throw new Exception("多仓数据无效（缺少 urls 字段）");
                }
            }

            checkJson(id, config, callback, object);

            if (taskId.get() == id && config.equals(this.config)) {
                config.update();
            }

        } catch (Throwable e) {
            e.printStackTrace();
            if (isCanceled(e)) return;
            if (taskId.get() != id) return;

            if (strictBoot && config == Config.vod() && !bootChecked) {
                String msg = Notify.getError(R.string.error_config_get, e);
                App.post(() -> {
                    Notify.show(
                            TextUtils.isEmpty(msg)
                                    ? "VOD 多仓加载失败，请检查网络或授权"
                                    : msg
                    );
                    if (App.activity() != null) {
                        App.activity().finish();
                    }
                });
                return;
            }

            if (callback != null) {
                App.post(() -> callback.error(Notify.getError(R.string.error_config_get, e)));
            }
        }
    }

    private void checkJson(int id, Config config, Callback callback, JsonObject object) {
        if (object.has("msg")) {
            App.post(() -> callback.error(object.get("msg").getAsString()));
        } else if (object.has("urls")) {
            parseDepot(id, config, callback, object);
        } else {
            parseConfig(id, config, callback, object);
        }
    }

    private void parseDepot(int id, Config config, Callback callback, JsonObject object) {
        List<Depot> items = Depot.arrayFrom(object.getAsJsonArray("urls").toString());
        if (items.isEmpty()) {
            throw new RuntimeException("多仓 urls 为空");
        }

        // ★ 覆盖旧 VOD 子仓
        Config.delete(config.getUrl());
        List<Config> configs = new ArrayList<>();
        for (Depot item : items) {
            configs.add(Config.find(item, 0));
        }

        // ★ 标记：启动多仓校验已完成
        bootChecked = true;

        loadConfig(id, this.config = configs.get(0), callback);
    }

    private void parseConfig(int id, Config config, Callback callback, JsonObject object) {
        try {
            initList(object);
            initLive(config, object);
            initWall(config, object);
            initSite(config, object);
            initParse(config, object);

            config.logo(Json.safeString(object, "logo"));
            String notice = Json.safeString(object, "notice");

            if (taskId.get() != id) return;

            App.post(() -> callback.success(notice));
            App.post(callback::success);

        } catch (Throwable e) {
            e.printStackTrace();
            if (taskId.get() != id) return;
            App.post(() -> callback.error(Notify.getError(R.string.error_config_parse, e)));
        }
    }

    private void initList(JsonObject object) {
        setHeaders(Header.arrayFrom(object.getAsJsonArray("headers")));
        setProxy(Proxy.arrayFrom(object.getAsJsonArray("proxy")));
        setRules(Rule.arrayFrom(object.getAsJsonArray("rules")));
        setDoh(Doh.arrayFrom(object.getAsJsonArray("doh")));
        setFlags(Json.safeListString(object, "flags"));
        setHosts(Json.safeListString(object, "hosts"));
        setAds(Json.safeListString(object, "ads"));
    }

    private void initLive(Config config, JsonObject object) {
        if (Json.isEmpty(object, "lives")) return;
        Config temp = Config.find(config, 1).save();
        boolean sync = LiveConfig.get().needSync(config.getUrl());
        if (sync) LiveConfig.get().config(temp.update()).parse(object);
    }

    private void initWall(Config config, JsonObject object) {
        if (Json.isEmpty(object, "wallpaper")) return;
        wall = Json.safeString(object, "wallpaper");
        Config temp = Config.find(wall, config.getName(), 2).save();
        boolean sync = WallConfig.get().needSync(wall);
        if (sync) WallConfig.get().config(temp.update());
    }

    private void initSite(Config config, JsonObject object) {
        String spider = Json.safeString(object, "spider");
        BaseLoader.get().parseJar(spider, true);

        setSites(
                Json.safeListElement(object, "sites")
                        .stream()
                        .map(e -> Site.objectFrom(e, spider))
                        .distinct()
                        .collect(Collectors.toCollection(ArrayList::new))
        );

        Map<String, Site> items = Site.findAll()
                .stream()
                .collect(Collectors.toMap(Site::getKey, Function.identity()));

        getSites().forEach(site -> site.sync(items.get(site.getKey())));

        setHome(
                config,
                getSites().isEmpty()
                        ? new Site()
                        : getSites().stream()
                        .filter(item -> item.getKey().equals(config.getHome()))
                        .findFirst()
                        .orElse(getSites().get(0)),
                false
        );
    }

    private void initParse(Config config, JsonObject object) {
        setParses(
                Json.safeListElement(object, "parses")
                        .stream()
                        .map(Parse::objectFrom)
                        .distinct()
                        .collect(Collectors.toCollection(ArrayList::new))
        );

        setParse(
                config,
                getParses().isEmpty()
                        ? new Parse()
                        : getParses().stream()
                        .filter(item -> item.getName().equals(config.getParse()))
                        .findFirst()
                        .orElse(getParses().get(0)),
                false
        );
    }

    public List<Site> getSites() {
        return sites == null ? Collections.emptyList() : sites;
    }

    private void setSites(List<Site> sites) {
        this.sites = sites;
    }

    public List<Parse> getParses() {
        return parses == null ? Collections.emptyList() : parses;
    }

    private void setParses(List<Parse> parses) {
        if (!parses.isEmpty()) parses.add(0, Parse.god());
        this.parses = parses;
    }

    public List<Doh> getDoh() {
        List<Doh> items = Doh.get(App.get());
        if (doh == null) return items;
        items.removeAll(doh);
        items.addAll(doh);
        return items;
    }

    private void setDoh(List<Doh> doh) {
        this.doh = doh;
    }

    public List<Rule> getRules() {
        return rules == null ? Collections.emptyList() : rules;
    }

    private void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    private void setHeaders(List<Header> headers) {
        OkHttp.responseInterceptor().addAll(headers);
    }

    private void setProxy(List<Proxy> proxy) {
        OkHttp.authenticator().addAll(proxy);
        OkHttp.selector().addAll(proxy);
    }

    private void setHosts(List<String> hosts) {
        OkHttp.dns().addAll(hosts);
    }

    private void setFlags(List<String> flags) {
        this.flags = flags;
    }

    private void setAds(List<String> ads) {
        this.ads = ads;
    }

    private void setHome(Config config, Site site, boolean save) {
        home = site;
        home.setActivated(true);
        config.home(home.getKey());
        if (save) config.save();
        getSites().forEach(item -> item.setActivated(home));
    }

    private void setParse(Config config, Parse parse, boolean save) {
        this.parse = parse;
        this.parse.setActivated(true);
        config.parse(parse.getName());
        getParses().forEach(item -> item.setActivated(parse));
        if (save) config.save();
    }
}

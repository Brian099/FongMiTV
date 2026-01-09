# 影視 fork from [FongMi/TV](https://github.com/FongMi/TV)

此项目基于fongMi/TV，内置了一个源，同时修改了app更新json和apk的位置，【内置源地址为：http://192.168.1.249:1666】

在原项目的基础上隐藏了一部分敏感信息显示及远程推送源等功能，

在原项目基础上修改了一些启动流程，当app启动时，请求服务器时附带了android_id和device_name参数，以实现服务器端根据参数进行授权管理。



**app启动顺序说明：**

app启动时 homeactivity.java 进行数据初始化-》decoder.java 获取远程数据并解密-》vodconfig.java 解析解密后的数据并进行数据库操作。

项目中上传了接口示例 api.php，可根据此示例进行后台适配。



[原项目说明]

### 基於 CatVod 項目

https://github.com/CatVodTVOfficial/CatVodTVJarLoader

### 點播欄位

| 欄位名稱       | 預設值  | 說明   | 其他         |
|------------|------|------|------------|
| searchable | 1    | 是否搜索 | 0：關閉；1：啟用  |
| changeable | 1    | 是否換源 | 0：關閉；1：啟用  |
| quickserch | 1    | 是否快搜 | 0：關閉；1：啟用  |
| indexs     | 0    | 是否聚搜 | 0：關閉；1：啟用  |
| hide       | 0    | 是否隱藏 | 0：顯示；1：隱藏  |
| timeout    | 15   | 播放超時 | 單位：秒       |
| header     | none | 請求標頭 | 格式：json    |
| click      | none | 點擊js | javascript |

### 直播欄位

| 欄位名稱     | 預設值   | 說明    | 其他         |
|----------|-------|-------|------------|
| ua       | none  | 用戶代理  |            |
| origin   | none  | 來源    |            |
| referer  | none  | 參照地址  |            |
| epg      | none  | 節目地址  |            |
| logo     | none  | 台標地址  |            |
| pass     | false | 是否免密碼 |            |
| boot     | false | 是否自啟動 |            |
| timeout  | 15    | 播放超時  | 單位：秒       |
| header   | none  | 請求標頭  | 格式：json    |
| click    | none  | 點擊js  | javascript |
| catchup  | none  | 回看參數  |            |
| timeZone | none  | 時區    |            |

### 樣式

| 欄位名稱  | 值    | 說明  |
|-------|------|-----|
| type  | rect | 矩形  |
|       | oval | 橢圓  |
|       | list | 列表  |
| ratio | 0.75 | 3：4 |
|       | 1.33 | 4：3 |

直式

```json
{
  "style": {
    "type": "rect"
  }
}
```

橫式

```json
{
  "style": {
    "type": "rect",
    "ratio": 1.33
  }
}
```

正方

```json
{
  "style": {
    "type": "rect",
    "ratio": 1
  }
}
```

正圓

```json
{
  "style": {
    "type": "oval"
  }
}
```

橢圓

```json
{
  "style": {
    "type": "oval",
    "ratio": 1.1
  }
}
```

### API

播放控制

type 包含 stop、prev、next、loop、play、pause、replay
```
http://127.0.0.1:9978/action?do=control&type=next
```

刷新詳情

```
http://127.0.0.1:9978/action?do=refresh&type=detail
```

刷新播放

```
http://127.0.0.1:9978/action?do=refresh&type=player
```

刷新直播

```
http://127.0.0.1:9978/action?do=refresh&type=live
```

推送字幕

```
http://127.0.0.1:9978/action?do=refresh&type=subtitle&path=http://xxx
```

推送彈幕

```
http://127.0.0.1:9978/action?do=refresh&type=danmaku&path=http://xxx
```

新增緩存字串

```
http://127.0.0.1:9978/cache?do=set&key=xxx&value=xxx
```

取得緩存字串

```
http://127.0.0.1:9978/cache?do=get&key=xxx
```

刪除緩存字串

```
http://127.0.0.1:9978/cache?do=del&key=xxx
```

### Proxy

支持 http, https, socks4, socks5

```
scheme://username:password@host:port
```

配置新增 proxy 可指定代理
靠前的 host 匹配到則使用該代理

```json
{
  "spider": "",
  "proxy": [
    {
      "name": "自訂",
      "hosts": [
        "googlevideo.com",
        "raw.githubusercontent.com"
      ],
      "urls": [
        "http://127.0.0.1:7890"
      ]
    },
    {
      "name": "全局",
      "hosts": [
        ".*."
      ],
      "urls": [
        "socks5://127.0.0.1:7891"
      ]
    }
  ]
}
```

### Hosts

```json
{
  "spider": "",
  "hosts": [
    "cache.ott.*.itv.cmvideo.cn=base-v4-free-mghy.e.cdn.chinamobile.com"
  ]
}
```

### Headers

```json
{
  "spider": "",
  "headers": [
    {
      "host": "gslbserv.itv.cmvideo.cn",
      "header": {
        "User-Agent": "okhttp/3.12.13",
        "Referer": "test"
      }
    }
  ]
}
```

### 爬蟲本地代理

Java

```
proxy://
```

```
Proxy.getUrl(boolean local)
```

Python

```
proxy://do=py
```

```
getProxyUrl(boolean local)
```

JS

```
proxy://do=js
```

```
getProxy(boolean local)
```

### 配置範例

[本地/線上](other/sample/config.json)

### 飛機群

[討論群組](https://t.me/fongmi_official)  
[發布頻道](https://t.me/fongmi_release)

### 贊助

![photo_2024-01-10_11-39-12](https://github.com/FongMi/TV/assets/3471963/fdc12771-386c-4d5d-9a4d-d0bec0276fa7)

### Star

[![Star History Chart](https://api.star-history.com/svg?repos=FongMi/TV&type=Date)](https://www.star-history.com/#FongMi/TV&Date)

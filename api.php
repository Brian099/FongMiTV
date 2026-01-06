<?php
// encrypt_return.php
// 直接输出加密后的十六进制字符串（符合 Java Decoder.cbc() 期待的结构）
// 依赖：php-openssl

// ---- 配置：修改以下 $key 和 $iv13 为你要使用的值 ----
$key  = 'mySecretKey123';    // 原始 key（会被转为小写并用字符 '0' 补齐到 16）
$iv13 = 'iv12345678901';     // 必须为 13 字节（会被转为小写并用字符 '0' 补齐到 16）
// ---------------------------------------------------------

// 要加密的 JSON（使用你提供的内容）
$dataArray = [
    "urls" => [
        ["name" => "🚀小盒子","url" => "http://xhztv.top/xhz"],
        ["name" => "🐔肥猫","url" => "http://我不是.肥猫.live/接口禁止贩卖"],
        ["name" => "🐟 摸 鱼","url" => "http://我不是.摸鱼儿.top"],
        ["name" => "🍯毒盒","url" => "https://毒盒.com/tv"],
        ["name" => "🍯PG线路","url" => "https://git.acwing.com/iduoduo/orange/-/raw/main/jsm.json"],
        ["name" => "🍯影视仓","url" => "https://download.kstore.space/download/2883/nzk/nzk0722.json"],
        ["name" => "🦆神仙线路","url" => "http://dp.sxtv.top:88/img"],
        ["name" => "🦆俊于","url" => "http://home.jundie.top:81/top98.json"],
        ["name" => "😹影视资源","url" => "http://bp.tvbox.cam"],
        ["name" => "😹玄珠","url" => "https://jihulab.com/xuanzhuapp/xzys/-/raw/main/xzvip.json"],
        ["name" => "😹吾爱","url" => "http://52pan.top:81/api/v3/file/get/174964/%E5%90%BE%E7%88%B1%E8%AF%84%E6%B5%8B.m3u?sign=rPssLoffquDXszCARt6UNF8MobSa1FA27XomzOluJBY%3D%3A0"],
        ["name" => "😹动漫城","url" => "https://www.yingm.cc/dm/dm.json"],
        ["name" => "🚀喵影视","url" => "http://meowtv.cn/tv"],
        ["name" => "🚀摸鱼","url" => "http://我不是.摸鱼儿.top"],
        ["name" => "🚀小马","url" => "https://szyyds.cn/tv/x.json"],
        ["name" => "🦉飘零","url" => "https://100km.top/0"],
        ["name" => "🐹dxawi","url" => "https://dxawi.github.io/0/0.json"],
        ["name" => "🍯HG","url" => "https://api.hgyx.vip/hgyx.json"],
    ]
];

$plaintext = json_encode($dataArray, JSON_UNESCAPED_UNICODE);

// 根据 Java 实现：先把 key/iv 转为小写，然后 padEnd 到 16（用字符 '0'）作为 AES key/iv
$keyLower  = strtolower($key);
$iv13Lower = strtolower($iv13);

$keyPadded = str_pad($keyLower, 16, '0'); // AES key (16 bytes)
$ivPadded  = str_pad($iv13Lower, 16, '0'); // AES iv (16 bytes, Java 用最后 13 字符补到 16)

// AES-128-CBC + PKCS5Padding (OpenSSL 的 PKCS7 与之等价)
$cipherMethod = 'AES-128-CBC';
$cipherRaw = openssl_encrypt($plaintext, $cipherMethod, $keyPadded, OPENSSL_RAW_DATA, $ivPadded);
if ($cipherRaw === false) {
    header('Content-Type: text/plain; charset=utf-8', true, 500);
    echo 'ENCRYPT_ERROR';
    exit;
}

// 构造最终的 hex payload：hex("$#") + hex(key原始小写) + hex("#$") + hex(ciphertext) + hex(iv13原始小写)
$hexPrefix1 = bin2hex("$#");               // "2423"
$hexKey     = bin2hex($keyLower);          // 原始 key（小写，不做填充）
$hexSep     = bin2hex("#$");               // "2324"
$hexCipher  = bin2hex($cipherRaw);         // 密文
$hexIv13    = bin2hex($iv13Lower);         // 原始 iv13（小写，13 字节）

$finalHex = $hexPrefix1 . $hexKey . $hexSep . $hexCipher . $hexIv13;

// 仅返回加密后的十六进制字符串
header('Content-Type: text/plain; charset=utf-8');
echo $finalHex;
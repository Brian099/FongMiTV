<?php
if (!function_exists('getallheaders')) {
    function getallheaders() {
        $headers = [];
        foreach ($_SERVER as $name => $value) {
            if (strpos($name, 'HTTP_') === 0) {
                $key = str_replace(' ', '-', ucwords(strtolower(str_replace('_', ' ', substr($name, 5)))));
                $headers[$key] = $value;
            }
        }
        return $headers;
    }
}

// 设置响应头，确保输出友好显示
header('Content-Type: text/html; charset=utf-8');

// 日志目录
$logDir = __DIR__ . '/logs';
if (!is_dir($logDir)) {
    mkdir($logDir, 0777, true);
}

// 检查是否请求最新日志
if (isset($_GET['latest'])) {
    displayLatestLog($logDir);
    exit;
}

// 用当前时间命名日志文件
$logFile = $logDir . '/request_' . date('Ymd_His') . '_' . uniqid() . '.html';

// HTML 开头
$html = "<!DOCTYPE html><html lang='zh-CN'><head>
<meta charset='UTF-8'>
<title>Request Log</title>
<style>
body { font-family: Consolas, monospace; background:#f8f9fa; color:#212529; padding:20px; }
h2 { border-bottom:1px solid #ccc; padding-bottom:5px; margin-top:20px; justify-content:space-between; align-items:center; }
h2 button { font-size:12px; padding:2px 6px; cursor:pointer; }
pre {
    background:#fff;
    border:1px solid #ddd;
    padding:10px;
    border-radius:5px;
    overflow:auto;
    white-space: pre-wrap;      /* 保留缩进同时自动换行 */
    word-wrap: break-word;      /* 长单词或长URL自动换行 */
}
</style>
<script>
function toggleSection(id, btn) {
    const el = document.getElementById(id);
    if (el.style.display === 'none') {
        el.style.display = 'block';
        btn.textContent = '折叠';
    } else {
        el.style.display = 'none';
        btn.textContent = '展开';
    }
}
</script>
</head><body>
<div style='margin-bottom:20px;'>
    <a href='?latest' style='background:#007bff; color:white; padding:8px 15px; text-decoration:none; border-radius:4px;'>查看最新日志</a>
</div>\n";

function section($title, $content, $id) {
    return "<h2>$title <button onclick=\"toggleSection('$id', this)\">折叠</button></h2>
    <pre id=\"$id\">" . htmlspecialchars($content) . "</pre>\n";
}

// === Client Information ===
$clientInfo  = "IP Address: " . $_SERVER['REMOTE_ADDR'] . "\n";
$clientInfo .= "Port: " . $_SERVER['REMOTE_PORT'] . "\n";
$clientInfo .= "User Agent: " . ($_SERVER['HTTP_USER_AGENT'] ?? 'N/A') . "\n";
$html .= section("Client Information", $clientInfo, "clientInfo");

// === Request Information ===
$requestInfo  = "Request Method: " . $_SERVER['REQUEST_METHOD'] . "\n";
$requestInfo .= "Request URI: " . $_SERVER['REQUEST_URI'] . "\n";
$requestInfo .= "Query String: " . ($_SERVER['QUERY_STRING'] ?? 'N/A') . "\n";
$html .= section("Request Information", $requestInfo, "requestInfo");

// === Headers ===
$headers = "";
foreach (getallheaders() as $name => $value) {
    $headers .= "$name: $value\n";
}
$html .= section("Headers", $headers, "headers");

// === Server Information ===
$serverInfo  = "Server Name: " . $_SERVER['SERVER_NAME'] . "\n";
$serverInfo .= "Server Software: " . $_SERVER['SERVER_SOFTWARE'] . "\n";
$serverInfo .= "Server Protocol: " . $_SERVER['SERVER_PROTOCOL'] . "\n";
$serverInfo .= "Server Port: " . $_SERVER['SERVER_PORT'] . "\n";
$serverInfo .= "Document Root: " . $_SERVER['DOCUMENT_ROOT'] . "\n";
$html .= section("Server Information", $serverInfo, "serverInfo");

// === GET Parameters ===
$getParams = empty($_GET) ? "No GET parameters." : print_r($_GET, true);
$html .= section("GET Parameters", $getParams, "getParams");

// === POST Parameters ===
$postParams = empty($_POST) ? "No POST parameters." : print_r($_POST, true);
$html .= section("POST Parameters", $postParams, "postParams");

// === JSON Body 捕获 ===
$rawInput = file_get_contents("php://input");
if (!empty($rawInput)) {
    // 在 Raw Request Body 标题旁添加两个按钮
    $extraButtons = '<button onclick="window.open(\'https://www.toolhelper.cn/EncodeDecode/Base64\', \'_blank\')">解析Base64</button>  ';
    $extraButtons .= '<button onclick="window.open(\'https://www.toolhelper.cn/JSON/JSONFormat?type=2\', \'_blank\')">格式化 JSON</button>';

    // 修改 section 函数调用，支持额外按钮
    $html .= "<h2>Raw Request Body $extraButtons <button onclick=\"toggleSection('rawBody', this)\">折叠</button></h2>";
    $html .= "<pre id='rawBody'>" . htmlspecialchars($rawInput) . "</pre>";

    // 如果是合法 JSON，也显示解析后的内容
    $decoded = json_decode($rawInput, true);
    if (json_last_error() === JSON_ERROR_NONE) {
        $prettyJson = json_encode($decoded, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        $html .= section("Parsed JSON", $prettyJson, "parsedJson");
    }
}

// === Uploaded Files ===
if (!empty($_FILES)) {
    $filesInfo = print_r($_FILES, true);
} else {
    $filesInfo = "No files uploaded.";
}
$html .= section("Uploaded Files", $filesInfo, "filesInfo");

// HTML 结尾
$html .= "</body></html>";

// 保存到日志文件
file_put_contents($logFile, $html);

// 同时输出到浏览器
echo $html;

/**
 * 显示最新的日志文件
 */
function displayLatestLog($logDir) {
    // 获取所有日志文件
    $files = glob($logDir . '/request_*.html');
    
    if (empty($files)) {
        echo "<h2>No log files found</h2>";
        return;
    }
    
    // 按修改时间排序，最新的在前
    usort($files, function($a, $b) {
        return filemtime($b) - filemtime($a);
    });
    
    // 获取最新的日志文件
    $latestFile = $files[0];
    
    // 读取并输出最新日志内容
    if (file_exists($latestFile)) {
        $content = file_get_contents($latestFile);
        
        // 更新标题以显示这是最新日志
        $content = str_replace(
            '<title>Request Log</title>', 
            '<title>Latest Request Log</title>', 
            $content
        );
        
        // 添加提示信息
        $notice = "<div style='background:#d4edda; color:#155724; padding:10px; margin-bottom:20px; border:1px solid #c3e6cb; border-radius:5px;'>
                    <strong>现在显示日志文件[非本页本次请求的信息]:</strong> " . basename($latestFile) . " (创建时间: " . date('Y-m-d H:i:s', filemtime($latestFile)) . ")
                  </div>";
        
        $content = str_replace('<body>', '<body>' . $notice, $content);
        
        echo $content;
    } else {
        echo "<h2>Error reading latest log file</h2>";
    }
}
?>
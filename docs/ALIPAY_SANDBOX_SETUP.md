# 支付宝沙箱配置说明

系统保留两种演示方式：支付宝沙箱支付和本地模拟支付。支付宝沙箱未启用或配置不完整时，系统只显示本地模拟支付。

## 一、从支付宝开放平台准备信息

在支付宝开放平台进入沙箱环境，准备以下信息：

1. 沙箱应用 APPID。
2. 当前沙箱网关地址。
3. PKCS8 格式的应用私钥。
4. 与应用公钥匹配的支付宝公钥。
5. 沙箱买家账号及登录密码、支付密码。
6. 可选的沙箱卖家 PID，用于进一步校验异步通知中的 `seller_id`。

应用私钥不得提交到 Git，也不要写进 `application-dev.yml` 或 `application-prod.yml`。

## 二、执行数据库迁移

首次使用该分支时执行：

```powershell
$env:MYSQL_PWD = '你的MySQL密码'
$dbClient = 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe'
Get-Content -Raw -Encoding UTF8 '.\sql\migration\V004__alipay_sandbox_payment.sql' |
  & $dbClient -uroot -D db_exam --default-character-set=utf8mb4
```

## 三、配置后端进程环境变量

推荐把应用私钥和支付宝公钥分别保存在项目目录之外的纯文本文件中，再通过环境变量读取，避免私钥进入 PowerShell 历史记录：

```powershell
$env:ALIPAY_SANDBOX_ENABLED = 'true'
$env:ALIPAY_GATEWAY_URL = '从沙箱控制台复制的网关地址'
$env:ALIPAY_APP_ID = '沙箱APPID'
$env:ALIPAY_PRIVATE_KEY = (Get-Content -Raw 'D:\alipay-secrets\app-private-key.txt').Trim()
$env:ALIPAY_PUBLIC_KEY = (Get-Content -Raw 'D:\alipay-secrets\alipay-public-key.txt').Trim()
$env:ALIPAY_SELLER_ID = '沙箱卖家PID，可留空'
$env:ALIPAY_RETURN_URL = 'http://localhost:8080/api/payment-orders/alipay/return'
$env:ALIPAY_FRONTEND_RETURN_URL = 'http://localhost:9527/#/my-payments'
```

以上环境变量只对当前 PowerShell 窗口及从该窗口启动的后端进程生效。

## 四、配置异步通知地址

支付宝服务器无法访问 `localhost`。需要使用 cpolar 等内网穿透工具，把本机后端 8080 端口映射为公网 HTTPS 地址，然后设置：

```powershell
$env:ALIPAY_NOTIFY_URL = 'https://你的公网域名/api/payment-orders/alipay/notify'
```

没有公网通知地址时仍可以完成沙箱付款，并在“我的缴费”页面点击“查询支付结果”主动同步状态；正式演示建议同时配置异步通知。

## 五、验收场景

1. 未配置沙箱：页面只显示“本地模拟”。
2. 配置沙箱：页面显示“支付宝沙箱支付”“查询支付结果”和“本地模拟”。
3. 沙箱付款成功：同步返回参数验签、订单号和金额校验通过后，订单变为“已缴费”，支付方式显示“支付宝沙箱”。
4. 重复异步通知：不会生成重复支付记录。
5. 金额或签名不匹配：订单不能变为已缴费。
6. 沙箱不可用：仍可使用本地模拟完成答辩演示。

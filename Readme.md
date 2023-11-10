# Link

![](https://wedoc.canway.net/imgs/img/嘉为蓝鲸.jpg)

Link 是一个用于 Java 应用程序的 Java 代理附加工具。

> [apm-agent-java](https://github.com/elastic/apm-agent-java)

## 要求

* 在类 Unix 系统（如 Linux、macOS 等）上，支持 `hostspot` JVM（如 OpenJDK 和 Oracle JDK），执行用户必须与 JVM 进程用户相同，或者可以切换用户为与
  JVM 进程用户相同的用户。
* 在 Windows 上，仅支持 `hostspot` JVM（如 OpenJDK 和 Oracle JDK）。

## 选项

下表列出了 Link 工具的可用选项及其描述：

| 选项                  | 参数                   | 描述                 |
|---------------------|----------------------|--------------------|
| -h, --help          | 无                    | 显示帮助信息             |
| -l, --list          | 无                    | 列出与发现规则匹配的 JVM     |
| -v, --list-vmargs   | 无                    | 在列出 JVM 时包括 JVM 参数 |
| -c, --continuous    | 无                    | 连续附加到匹配的 JVM       |
| --no-fork           | 无                    | 附加到 JVM 时不创建新进程    |
| --include-all       | 无                    | 包括所有 JVM 进行附加      |
| --include-pid       | pid                  | 包括指定 PID 的进程进行附加   |
| --include-main      | pattern              | 包括指定的主类模式进行附加      |
| --exclude-main      | pattern              | 排除指定的主类模式进行附加      |
| --include-vmarg     | pattern              | 包括指定的 JVM 参数模式进行附加 |
| --exclude-vmarg     | pattern              | 排除指定的 JVM 参数模式进行附加 |
| --include-user      | pattern              | 包括指定用户的进程进行附加      |
| --exclude-user      | pattern              | 排除指定用户的进程进行附加      |
| -C, --config        | key=value            | 设置代理配置选项           |
| -A, --args-provider | args_provider_script | 设置用于提供代理参数的程序      |
| --agent-jar         | file                 | 设置代理 JAR 文件路径      |

## 示例

* 查看所有匹配规则的JVM进程

```
java -jar ./link.jar -l
```

* 为Tomcat注入OpenTelemetry的Java Agent

```
java -jar ./link.jar  --include-main org.apache.catalina.startup.Bootstrap --agent-jar ./opentelemetry-javaagent-all.jar
```

* 作为常驻进程，自动注入所有的JVM应用
```
java -jar ./link.jar --include-all --continuous --agent-jar ./opentelemetry-javaagent-all.jar
```


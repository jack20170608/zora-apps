# host-helper 部署包

此包仅包含使用 `zora-bin` 原版生命周期脚本的 Ansible playbook 与说明；运行时 JAR 在独立的 `package-service` 包中。不会安装或管理 systemd unit。

## 安装前提

- Linux、Bash、JDK 25 和 Ansible；应用由 `zora-bin` 的 `deploy.sh`、`start.sh`、`stop.sh`、`status.sh` 管理，不要求 systemd。
- 控制机上已有对应版本的 `-service.tar.gz` 压缩包。
- 目标机已由安全渠道创建 `/etc/host-helper/release-config/setenv`（root 所有、hosthelper 组可读，权限 0640），写入 `HOST_HELPER_CONFIG_DIR=/etc/host-helper/config`，可选 `PORT=...`。**这是 zora-bin 信任并 source 的 Bash 文件**，只允许可信运维修改；不要把凭据或环境文件提交到仓库。
- 在 `/etc/host-helper/config/application-prod.conf` 中提供生产配置（root 所有、hosthelper 组可读，权限 0640）。服务 JAR 不包含 `local`/`sit` 配置或 JWT 密钥；JWT 私钥、公钥应指向 `hosthelper` 可读的受保护文件。生产配置必须提供所选环境需要的用户和主机信息，不要直接使用示例账户。

## 使用

将 `package-deploy` 包解压到控制机，准备 Ansible inventory（组名 `host_helper`），然后从解压目录运行：

```bash
ansible-playbook -i inventory.ini ansible/install.yml -e "host_helper_version=1.0.0-SNAPSHOT host_helper_env=prod service_archive=/absolute/path/host-helper-dist-1.0.0-SNAPSHOT-service.tar.gz"
```

playbook 拒绝缺失环境配置的机器，直接将包内 `hosthelper/<version>/app.jar`、`lib/`、`bin/` 解压到 `/opt/hosthelper/<version>/`，并把受控的 `/etc/host-helper/release-config/` 复制到版本目录的 `config/`。随后在 `hosthelper` 账户下调用包内原版 `zora-bin/deploy.sh --activate <version>` 建立 `active` 软链，并按需启动；`logs/`、`run/` 由脚本自动创建。重复发布同版本保持幂等；回滚用 `bash /opt/hosthelper/active/bin/deploy.sh --activate <旧版本>`。可直接运行 `bash /opt/hosthelper/active/bin/{start,stop,status}.sh` 管理应用。`env.tag` 放在 `/opt/hosthelper/` 根目录，生产密钥不得进入归档；发布前请备份并审查外部状态/数据。


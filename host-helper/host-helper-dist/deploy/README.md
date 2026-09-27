# host-helper 部署包

此包仅包含使用 `zora-bin` 原版生命周期脚本的 Ansible playbook 与说明；运行时 JAR 在独立的 `package-service` 包中。不会安装或管理 systemd unit。

## 安装前提

- Linux、Bash、JDK 25 和 Ansible；应用由 `zora-bin` 的 `deploy.sh`、`start.sh`、`stop.sh`、`status.sh` 管理，不要求 systemd。
- 控制机上已有对应版本的 `-service.tar.gz` 压缩包。
- 目标机已由安全渠道创建 `/etc/host-helper/release-config/setenv`（root 所有、hosthelper 组可读，权限 0640）。playbook 把它复制到每个版本的 `config/`；应将 `HOST_HELPER_CONFIG_DIR` 设置为当前脚本所在目录的绝对路径（示例：`export HOST_HELPER_CONFIG_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"`），可选 `PORT=...`。**这是 zora-bin 信任并 source 的 Bash 文件**；`setenv-sit`、`setenv-prod` 同理可选，`application.sh` 不会自动执行。只允许可信运维修改这些文件，不要把凭据或环境文件提交到仓库。
- 在 `/etc/host-helper/config/application-<env>.conf` 中提供所选环境的配置（root 所有、hosthelper 组可读，权限 0640）。例如 SIT 使用 `application-sit.conf`。服务 JAR 不包含 `local`/`sit` 配置或 JWT 密钥；JWT 私钥、公钥应指向 `hosthelper` 可读的受保护文件。不要直接使用仓库里的示例账户。

## 应用部署后的目录结构

以 `host_helper_root=/opt/hosthelper`、当前激活版本 `1.2`、环境 `sit` 为例（版本号和根目录以实际部署参数为准）：

```text
/opt/hosthelper/
├── env.tag                 # Ansible 写入 sit；手动使用 zora-bin 时可改用 APP_ENV
├── active -> 1.2/          # deploy.sh --activate 管理的当前版本软链
├── 1.0/                    # 保留的旧版本
├── 1.1/
├── 1.2/
│   ├── app.jar             # service 包提供
│   ├── lib/                # service 包提供；本项目运行时依赖必需
│   ├── config/             # 部署时由受控配置源填充，不在 service 包内
│   │   ├── setenv          # 通用环境变量；本 Ansible 部署要求提供
│   │   ├── setenv-sit      # 可选：仅 SIT 加载
│   │   ├── setenv-uat      # 可选：仅 UAT 加载
│   │   ├── setenv-prod     # 可选：仅 PROD 加载
│   │   ├── application.sh  # 可选：应用自有文件，不会自动执行
│   │   └── application-sit.conf  # SIT 的 HOCON 配置；部署时提供
│   └── bin/                # service 包直接提取的 zora-bin 原版脚本
│       ├── lifecycle.sh
│       ├── start.sh
│       ├── stop.sh
│       ├── status.sh
│       └── deploy.sh
├── logs/                   # 脚本按需创建
└── run/                    # 脚本按需创建，包含 PID/锁文件
```

service 归档的顶层**只有 `<version>/`**，不包含 `/opt/hosthelper`、`env.tag`、`active`、`logs/` 或 `run/`。SIT 示例之外，UAT/PROD 使用对应的 `application-uat.conf`/`application-prod.conf`，不能复用 SIT 配置。历史版本只有在确实发布过相应版本后才会出现。

## 使用

部署包采用 Ansible 标准的 playbook、role、inventory 目录分工：

```text
ansible/
├── ansible.cfg
├── inventories/
│   ├── example/hosts.ini
│   ├── sit/
│   │   ├── hosts.ini
│   │   └── group_vars/sit.yml
│   └── uat/
│       ├── hosts.ini
│       └── group_vars/uat.yml
├── playbooks/deploy.yml
└── roles/hosthelper/
	├── defaults/main.yml
	└── tasks/{main,preflight,install,activate}.yml
```

将 `-deploy.tar.gz` 解压到控制机，准备自己的 inventory（`inventories/example/hosts.ini` 只是无凭据的示例），在解压目录的 `ansible/` 下执行，使 `ansible.cfg` 的 `roles_path` 生效：

```bash
cd ansible
ansible-playbook -i /path/to/inventory.ini playbooks/deploy.yml -e "host_helper_version=1.0.0-SNAPSHOT host_helper_env=prod service_archive=/absolute/path/host-helper-dist-1.0.0-SNAPSHOT-service.tar.gz"
```

上面的生产示例要求目标主机属于 `prod` 环境组，同时该组属于 `host_helper`；只设置 `host_helper_env=prod`、却没有对应 inventory 分组时，预检会拒绝部署。

SIT 清单当前使用 `sit-30/31/32`（10.10.10.30/31/32），并通过 `group_vars/sit.yml` 选择 `host_helper_env: sit`。请在运行前核对清单与实际 SSH 可达性及部署授权；私钥和口令不得写入清单。仅部署 SIT：

```bash
cd ansible
ansible-playbook -i inventories/sit/hosts.ini playbooks/deploy.yml --limit sit -e "host_helper_version=1.0.0-SNAPSHOT service_archive=/absolute/path/host-helper-dist-1.0.0-SNAPSHOT-service.tar.gz"
```

UAT 清单使用 `uat-188`（172.16.10.188）和 `uat-189`（172.16.10.189），由 `group_vars/uat.yml` 设置 `host_helper_env: uat`。SSH 用户、私钥和口令通过 SSH 配置或部署命令从受控渠道提供；不要将私钥写入仓库。仅部署 UAT：

```bash
cd ansible
ansible-playbook -i inventories/uat/hosts.ini playbooks/deploy.yml --limit uat -e "host_helper_version=1.0.0-SNAPSHOT service_archive=/absolute/path/host-helper-dist-1.0.0-SNAPSHOT-service.tar.gz"
```

注意：应用配置 `application.conf` 中的 UAT URL 目前仍是 `10.10.10.20/21:8000`；inventory 仅定义 SSH 部署目标，不会覆盖应用业务 URL。发布前必须核对并通过受控的 `application-uat.conf` 明确设置所需主机 URL。

部署前 role 校验 `host_helper_env` 必须与目标主机的 inventory 环境组一致；首次部署时由 `ansible.builtin.copy` 把环境名（如 `sit`、`uat`、`prod`）写入 `{{ host_helper_root }}/env.tag`，供 `zora-bin` 读取。重复部署相同环境保持幂等；如果已有标记指向不同环境，立即拒绝部署，不会静默切换。环境迁移需另行审查并使用独立根目录或明确的迁移流程；`env.tag` 不是存放凭据的文件。

role 拒绝缺失环境配置的机器，直接将归档中的 `<version>/app.jar`、`lib/`、`bin/` 解压到可配置的 `host_helper_root`（默认 `/opt/hosthelper`），并把受控的 `/etc/host-helper/release-config/` 及 `application-<env>.conf` 复制到版本目录的 `config/`。随后在 `hosthelper` 账户下调用包内原版 `zora-bin/deploy.sh --activate <version>` 建立 `active` 软链，并按需启动；`logs/`、`run/` 由脚本自动创建。重复发布同版本保持幂等；回滚用 `bash /opt/hosthelper/active/bin/deploy.sh --activate <旧版本>`。可直接运行 `bash /opt/hosthelper/active/bin/{start,stop,status}.sh` 管理应用。`env.tag` 放在运行根目录，生产密钥不得进入归档；发布前请备份并审查外部状态/数据。


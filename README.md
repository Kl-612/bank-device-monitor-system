# 银行设备监控系统

## 项目概述
基于Spring Boot的银行网点设备监控管理系统，专为银行设备运维设计。将硬件监控与金融软件开发结合，模拟真实银行设备管理场景。

## 技术栈
| 技术 | 用途 | 版本 |
|------|------|------|
| Spring Boot | 后端框架 | 4.0.1 |
| MyBatis | ORM框架 | 4.0.1 |
| MySQL | 数据库 | 8.0 |
| ECharts | 数据可视化 | 5.4.3 |
| Git | 版本控制 | 2.52 |

## 核心功能

### 设备管理
- **CRUD操作**：完整的设备增删改查
- **状态管理**：在线、离线、故障、维护四种状态
- **唯一性约束**：模拟银行设备序列号管理
- **搜索功能**：支持关键字、设备类型、支行多条件查询

### 银行业务规则
- **在线保护**：在线设备不可直接删除
- **变更审计**：状态变更必须记录原因
- **支行管理**：按银行组织结构分组
- **故障标记**：专业故障记录和跟踪

### 统计分析
- **支行健康度**：各支行设备在线率排名
- **保修预警**：未来30天到期设备提醒
- **故障分析**：MTTR（平均修复时间）统计
- **设备统计**：全方位设备运行数据概览

### 可视化界面
- **监控仪表板**：ECharts数据图表展示
- **实时监控大屏**：银行监控中心风格界面

## 数据库设计
```sql
-- 设备核心信息表
CREATE TABLE `device_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `device_id` varchar(64) NOT NULL COMMENT '设备唯一标识',
  `device_name` varchar(100) NOT NULL COMMENT '设备显示名称',
  `device_type` varchar(50) NOT NULL COMMENT '设备类型：ATM, VTM, 智能柜台, 网络设备',
  `vendor` varchar(100) DEFAULT NULL COMMENT '供应商',
  `model` varchar(100) DEFAULT NULL COMMENT '设备型号',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '设备IP地址',
  `location` varchar(200) NOT NULL COMMENT '部署位置',
  `branch` varchar(100) DEFAULT NULL COMMENT '所属支行',
  `status` varchar(20) DEFAULT 'offline' COMMENT '当前状态：online, offline, fault, maintenance',
  `install_date` date DEFAULT NULL COMMENT '安装日期',
  `warranty_period` int DEFAULT NULL COMMENT '保修期（月）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_location` (`location`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备核心信息表';

-- 状态监控日志表  
CREATE TABLE `device_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` varchar(64) NOT NULL COMMENT '关联的设备ID',
  `status` varchar(20) NOT NULL COMMENT '上报状态',
  `cpu_usage` float DEFAULT NULL COMMENT 'CPU使用率 (%)',
  `memory_usage` float DEFAULT NULL COMMENT '内存使用率 (%)',
  `disk_usage` float DEFAULT NULL COMMENT '磁盘使用率 (%)',
  `network_status` tinyint(1) DEFAULT '1' COMMENT '网络连通性：1正常，0异常',
  `temperature` float DEFAULT NULL COMMENT '设备温度（℃）',
  `cash_level` float DEFAULT NULL COMMENT '钞箱现金余量 (%) - 针对ATM',
  `log_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '日志记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_log_time` (`log_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态历史日志表，用于监控和数据分析';

-- 故障记录表
CREATE TABLE `device_fault_record` (
  `id` int NOT NULL AUTO_INCREMENT,
  `device_id` varchar(64) NOT NULL,
  `fault_code` varchar(50) NOT NULL COMMENT '故障代码',
  `fault_description` text COMMENT '故障详细描述',
  `fault_level` varchar(20) DEFAULT 'MEDIUM' COMMENT '故障等级：CRITICAL, HIGH, MEDIUM, LOW',
  `occurrence_time` datetime NOT NULL COMMENT '故障发生时间',
  `recovery_time` datetime DEFAULT NULL COMMENT '故障恢复时间',
  `downtime_duration` int GENERATED ALWAYS AS (TIMESTAMPDIFF(MINUTE, `occurrence_time`, `recovery_time`)) STORED COMMENT '宕机时长（分钟）- 计算列',
  `maintenance_person` varchar(100) DEFAULT NULL COMMENT '处理人',
  `solution` text COMMENT '解决方案',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_occurrence_time` (`occurrence_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备故障记录表，用于故障分析和运维管理';
```

## API接口
### 设备管理接口
| 方法 | 端点 | 功能 | 参数 | 状态码 |
|------|------|------|------|--------|
| **GET** | `/api/devices` | 获取所有设备列表 | 无 | 200成功 / 500失败 |
| **GET** | `/api/devices/{id}` | 按ID查询设备 | `id`| 200成功 /400参数错误/404不存在/ 500失败 |
| **GET** | `/api/devices/status/{status}` | 按状态查询设备 | `status` | 200成功 / 500失败 |
| **GET** | `/api/devices/branch/{branch}` | 按支行查询设备 | `branch`| 200成功 /400参数错误/ 500失败 |
| **POST** | `/api/devices` | 添加新设备 | `DeviceInfo对象` | 201创建 / 400参数错误 / 500失败 |
| **PUT** | `/api/devices/{id}` | 更新设备信息 | `id`, `DeviceInfo对象` | 200成功 / 400参数错误 / 500失败 |
| **DELETE** | `/api/devices/{id}` | 删除设备 | `id` | 200成功 / 400业务错误 / 500失败 |

### 状态变更接口
| 方法 | 端点 | 功能 | 请求体 | 状态码 |
|------|------|------|--------|--------|
| **PUT** | `/api/devices/{id}/status` | 变更设备状态 | `{"status":"新状态","reason":"变更原因"}` | 200成功 / 400参数错误 / 500失败 |
| **POST** | `/api/devices/{id}/mark-fault` | 标记设备故障 | `{"reason":"故障原因"}` | 200成功 / 400参数错误 / 500失败 |

### 查询搜索接口
| 方法 | 端点 | 功能 | 查询参数 | 状态码 |
|------|------|------|----------|--------|
| **GET** | `/api/devices/search` | 多条件搜索设备 | `keyword`, `deviceType`, `branch` | 200成功 / 500失败 |

### 统计分析接口
| 方法 | 端点 | 功能 | 返回类型 | 状态码 |
|------|------|------|----------|--------|
| **GET** | `/api/devices/stats/summary` | 设备统计概览 | `Map<String, Object>` | 200成功 / 500失败 |
| **GET** | `/api/devices/stats/warranty-alert` | 保修预警设备列表 | `List<Map<String, Object>>` | 200成功 |
| **GET** | `/api/devices/stats/fault-analysis` | 故障分析报告 | `Map<String, Object>` | 200成功 |
| **GET** | `/api/devices/stats/branch-health` | 支付健康度统计 | `Map<String, Object>` | 200成功 |


## 快速开始

### 1. 环境准备
```bash
# 需要安装
- JDK 17+
- MySQL 8.0+
- Maven 3.6+
```

### 2. 数据库配置
```sql
CREATE DATABASE bank_monitor DEFAULT CHARSET utf8mb4;
```

### 3. 修改配置
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bank_monitor?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 4. 启动应用
```bash
# 方式一：Maven启动
mvn spring-boot:run

# 方式二：IDEA直接运行
# 运行BankMonitorApplication.java
```

### 5. 访问界面
- **监控仪表板**：http://localhost:8080/dashboard.html
- **实时监控大屏**：http://localhost:8080/status-board.html

##  项目结构
```
bank-monitor/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── kl/
│   │   │           ├── BankMonitorApplication.java
│   │   │           ├── controller/
│   │   │           │   └── DeviceController.java
│   │   │           ├── entity/
│   │   │           │   └── DeviceInfo.java
│   │   │           ├── mapper/
│   │   │           │   └── DeviceInfoMapper.java
│   │   │           └── service/
│   │   │               ├── DeviceService.java
│   │   │               └── impl/
│   │   │                   └── DeviceServiceImpl.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── dashboard.html
│   │       │   └── status-board.html
│   │       ├── templates/
│   │       └── application.yml
│   └── test/
├── target/
├── .gitattributes
├── .gitignore
├── HELP.md
└── pom.xml
```

## 项目亮点

### 技术深度
- **三层架构**：清晰的Controller-Service-Mapper分离
- **数据库优化**：合理索引+计算列+外键约束
- **异常处理**：完整的错误处理和事务管理
- **RESTful设计**：规范的API接口设计

### 业务理解
- **银行合规**：状态变更审计、操作日志记录
- **设备管理**：全生命周期跟踪，从安装到报废
- **运维分析**：SLA指标（在线率、MTTR）计算
- **主动预警**：保修到期提前提醒

### 工程实践
- **Git规范**：清晰的提交记录和分支管理
- **接口文档**：Postman测试集合完整
- **代码质量**：规范的命名和注释
- **前后端分离**：现代化架构设计

## 界面展示
```
监控仪表板：
├── 关键指标卡片（总设备数、在线率等）
├── 支行健康度柱状图
├── 保修预警表格
└── 故障分析图表

实时监控大屏：
├── 红黄绿状态指示灯
├── 支行筛选功能
├── 设备详细信息
└── 自动刷新机制
```

## 联系方式
- **GitHub**：https://github.com/Kl-612/bank-device-monitor-system
- **邮箱**：2545994834@qq.com
- **Issue**：欢迎提出问题或建议

---

**更新日期**：2024年1月  
**项目状态**：完整可用，达到生产级标准  
**适用岗位**：银行科技岗、Java后端开发、物联网开发

  ---
  # AI 面试助手

  基于 RAG 的 AI 面试模拟平台，支持简历智能解析、多轮对话面试、语音实时评估、多维度 AI 评分与游戏化激励体系。

  ## 技术栈

  **后端**
  - Java 17 + Spring Boot 3.3
  - Spring AI 1.0 + DashScope (通义千问)
  - Spring Data Redis + Redis Vector Store (RAG)
  - MySQL 8.0 + HikariCP
  - JWT 认证 + 拦截器鉴权

  **前端**
  - Vue 3 + Vite 5
  - Element Plus + Tailwind CSS
  - ECharts 数据可视化
  - GSAP 动画引擎
  - Web Speech API 语音识别

  **基础设施**
  - Docker + Docker Compose
  - Nginx 反向代理

  ## 核心功能

  ### AI 面试模拟
  - 4 位 AI 面试官（技术/HR/压力面/友好型），不同提问风格与评估策略
  - 多轮对话面试，支持文字与语音输入
  - 自适应难度系统，根据表现动态调整题目难度
  - 面试回放，完整还原对话过程与评分变化

  ### RAG 知识库
  - 简历/文档上传，自动解析 PDF 文本与图片
  - 基于 Redis Vector Store 的语义检索
  - 面试问答时自动匹配知识库上下文，提升回答准确性

  ### 智能评估
  - 多维度 AI 评分（专业深度、逻辑表达、STAR 法则、岗位匹配度）
  - ELO 评分系统，量化面试能力成长轨迹
  - 成长仪表盘，可视化技能树与薄弱环节分析

  ### 游戏化激励
  - 等级系统 + 经验值积累
  - 成就徽章体系
  - 每日任务 + 连续打卡
  - 训练计划自动生成

  ### 求职辅助
  - 岗位匹配分析，基于简历评估岗位适配度
  - 求职项目管理，追踪投递进度
  - 题库系统，按分类与难度筛选练习题

  ## 项目结构

       backend/                        Spring Boot 后端
          src/main/java/.../
            ai/                         AI 模块（Agent、Skills、Prompts）
            auth/                       认证鉴权
            config/                     配置类
            controller/                 16 个 REST Controller
            exception/                  全局异常处理
            model/                      数据模型
            rag/                        RAG 检索增强
            repository/                 数据访问层
            service/                    业务逻辑层
          src/test/                     单元测试
        
        frontend/                       Vue 3 前端
          src/
            api/                        14 个 API 模块
            components/                 30+ 组件
            composables/                12 个 Composables
            views/                      9 个页面
            styles/                     样式系统
        
        docker-compose.yml              容器编排
        scripts/                        启动脚本
